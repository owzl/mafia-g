package MafiaG;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class Server {
    static List<ClientHandler> clients = new ArrayList<ClientHandler>();
    static final int MAX_CLIENTS = 7;
    static int anonymousCounter = 1;
    static String geminiNickname = null;
    static int readyCount = 0;

    static Map<String, Integer> voteMap = new HashMap<String, Integer>();
    static Map<String, String> answers = new HashMap<String, String>();
    static String currentQuestion = "";
    static String currentQuestioner = "";
    static int questionTurn = 0;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(3579)) {
            System.out.println("서버가 시작되었습니다");

            while (true) {
                Socket socket = serverSocket.accept();
                if (clients.size() >= MAX_CLIENTS) {
                    socket.close();
                    continue;
                }

                ClientHandler handler = new ClientHandler(socket);
                handler.colorCode = getRandomColor();
                handler.nickname = "익명" + anonymousCounter++;
                clients.add(handler);

                if (geminiNickname == null) {
                    geminiNickname = handler.nickname; // 첫 접속 유저를 Gemini로 지정
                    System.out.println("🌟 Gemini는 " + geminiNickname);
                }

                handler.start();
                broadcastParticipants();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void broadcast(String msg) {
        for (ClientHandler client : clients) {
            try {
                client.send(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static void broadcastParticipants() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"PARTICIPANTS\",\"list\":[");
        for (int i = 0; i < clients.size(); i++) {
            sb.append("\"").append(clients.get(i).nickname).append("\"");
            if (i != clients.size() - 1) sb.append(",");
        }
        sb.append("]}");
        broadcast(sb.toString());
    }

    static String getRandomColor() {
        Random r = new Random();
        return String.format("#%06x", r.nextInt(0xFFFFFF + 1));
    }

    static void startNextQuestion() {
        if (questionTurn >= clients.size()) {
            startVotePhase();
            return;
        }

        ClientHandler questioner = clients.get(questionTurn);
        currentQuestioner = questioner.nickname;
        questioner.sendDirect("{\"type\":\"QUESTION_PHASE\",\"questioner\":\"" + currentQuestioner + "\"}");
        broadcast("{\"type\":\"QUESTION_PHASE\",\"questioner\":\"" + currentQuestioner + "\"}");

        questionTurn++;
    }

    static void startVotePhase() {
        broadcast("{\"type\":\"VOTE_PHASE\"}");
        // 20초 후 결과 처리
        new Timer().schedule(new TimerTask() {
            public void run() {
                processVoteResult();
            }
        }, 20000);
    }

    static void processVoteResult() {
        String mostVoted = null;
        int maxVotes = 0;
        for (String target : voteMap.keySet()) {
            int count = voteMap.get(target);
            if (count > maxVotes) {
                mostVoted = target;
                maxVotes = count;
            }
        }

        String result = "[결과] " + mostVoted + "이(가) " + maxVotes + "표로 지목됨 → ";
        boolean geminiCaught = geminiNickname.equals(mostVoted);
        if (geminiCaught) {
            result += "Gemini 적중  → 전원 승리!";
        } else {
            result += "Gemini 아님  → " + mostVoted + "의 단독 승리!";
        }

        broadcast("{\"type\":\"REVEAL_RESULT\", \"message\":\"" + result + "\"}");

        // DB 반영
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mafiag", "root", "0000"
            );

            for (ClientHandler c : clients) {
                int delta = 0;
                if (geminiCaught) {
                    delta = 2;
                    c.send("{\"type\":\"WIN_RESULT\"}");
                } else {
                    if (c.nickname.equals(mostVoted)) {
                        delta = 5;
                        c.send("{\"type\":\"WIN_RESULT\"}");
                    } else {
                        delta = -1;
                        c.send("{\"type\":\"LOSE_RESULT\"}");
                    }
                }

                PreparedStatement stmt = conn.prepareStatement("UPDATE Member SET score = score + ? WHERE nickname = ?");
                stmt.setInt(1, delta);
                stmt.setString(2, c.nickname);
                stmt.executeUpdate();
                stmt.close();
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler extends Thread {
        Socket socket;
        BufferedReader br;
        BufferedWriter bw;
        String nickname;
        String colorCode;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                // INIT 메시지 전송
                send("{\"type\":\"INIT\",\"nickname\":\"" + nickname + "\",\"color\":\"" + colorCode + "\"}");

                String msg;
                while ((msg = br.readLine()) != null) {
                    if (msg.contains("\"type\":\"READY\"")) {
                        readyCount++;
                        if (readyCount == clients.size() && clients.size() >= 3) {
                            broadcast("{\"type\":\"GAME_START\"}");
                            startNextQuestion();
                        }
                    } else if (msg.contains("\"type\":\"QUESTION_SUBMIT\"")) {
                        currentQuestion = extractValue(msg, "message");
                        broadcast("{\"type\":\"ANSWER_PHASE\"}");
                        answers.clear();
                        new Timer().schedule(new TimerTask() {
                            public void run() {
                                String fake = clients.get(new Random().nextInt(clients.size())).nickname;
                                if (!answers.containsKey(fake)) {
                                    answers.put(fake, "Gemini의 답변입니다.");
                                }

                                StringBuilder sb = new StringBuilder();
                                sb.append("{\"type\":\"REVEAL_RESULT\",\"question\":\"")
                                  .append(currentQuestion).append("\",\"answers\":[");
                                int i = 0;
                                for (String nick : answers.keySet()) {
                                    sb.append("{\"nickname\":\"").append(nick)
                                      .append("\",\"message\":\"").append(answers.get(nick)).append("\"}");
                                    if (++i < answers.size()) sb.append(",");
                                }
                                sb.append("]}");
                                broadcast(sb.toString());
                                startNextQuestion();
                            }
                        }, 20000);
                    } else if (msg.contains("\"type\":\"ANSWER_SUBMIT\"")) {
                        String answer = extractValue(msg, "message");
                        answers.put(nickname, answer);
                    } else if (msg.contains("\"type\":\"VOTE\"")) {
                        String target = extractValue(msg, "target");
                        voteMap.put(target, voteMap.getOrDefault(target, 0) + 1);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clients.remove(this);
                    broadcastParticipants(); //  퇴장 시에도 리스트 갱신
                    br.close();
                    bw.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        void send(String msg) throws IOException {
            bw.write(msg);
            bw.newLine();
            bw.flush();
        }

        void sendDirect(String msg) {
            try {
                send(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static String extractValue(String json, String key) {
        int idx = json.indexOf(key);
        if (idx == -1) return "";
        int start = json.indexOf("\"", idx + key.length() + 2);
        int end = json.indexOf("\"", start + 1);
        return json.substring(start + 1, end);
    }
}

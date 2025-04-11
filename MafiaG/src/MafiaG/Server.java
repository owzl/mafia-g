package MafiaG;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import MafiaG.ConGemini;

public class Server {
	static List<ClientHandler> clients = new ArrayList<>();
	static final int MAX_CLIENTS = 7;
	static int anonymousCounter = 1;
	static int readyCount = 0;

	static Map<String, Integer> voteMap = new HashMap<>();
	static Set<String> votedUsers = new HashSet<>();
	static Map<String, String> answers = new HashMap<>();
	static int questionCount = 0;
	static final int MAX_QUESTIONS = 5;

	static List<String> questionList = Arrays.asList("오늘 점심으로 뭘 먹을까요?", "당신이 제일 좋아하는 동물은?", "주말에 뭐하면 좋을까요?",
			"가장 기억에 남는 여행지는 어디인가요?", "요즘 즐겨 듣는 음악은 뭔가요?", "어릴 때 꿈은 무엇이었나요?", "요즘 빠진 취미는?", "당신은 Gemini인가요?",
			"딥시크에 대해 어떻게 생각하나요?", "스트레스를 받을 때 어떻게 해소하는 편인가요?", "강아지 vs 고양이, 어느 쪽을 더 좋아하세요?", "단맛 vs 짠맛?",
			"무서운 이야기나 영화를 잘 보는 편인가요?", "지금 바로 여행을 갈 수 있다면 가고 싶은 곳은? (국내/해외 상관없이)", "가장 좋아하는 요일과 그 이유는?",
			"당신이 갖고 싶은 초능력은?", "알람 소리 없이 아침에 잘 일어나나요?", "달콤한 음료 vs 달지 않은 음료?", "새 옷 사기 vs 새 신발 사기, 하나만 고른다면?",
			"오늘 아침에 일어나서 가장 먼저 한 일은 무엇인가요?", "자신은 계획적인 사람과 즉흥적인 사람 중 어디에 더 가깝다고 생각하나요?", "가장 좋아하는 아이스크림 맛은?",
			"영화관에서 영화 보기 vs 집에서 영화 보기?", "기억에 남는 영화나 드라마는?", "손글씨와 키보드 타이핑 어떤게 편하신가요?", "투명인간이 된다면 무엇을 하고싶으신가요?",
			"당신의 OOTD에 대해 설명해 주세요!", "커피 vs 차", "종이책 vs 전자책 어떤걸 선호하시나요?"
	);
	static List<String> usedQuestions = new ArrayList<>();
	static Random random = new Random();
	static String currentQuestion = "";

	static boolean resultRevealed = false;
	static boolean gameStarted = false;

	static ClientHandler geminiBot = null;

	public static void main(String[] args) {
		try (ServerSocket serverSocket = new ServerSocket(3579)) {
			System.out.println("서버가 시작되었습니다");

			geminiBot = new GeminiBot("익명" + anonymousCounter++, getRandomColor());
			clients.add(geminiBot);

			while (true) {
				Socket socket = serverSocket.accept();
				socket.setSoTimeout(600000);
				if (clients.size() >= MAX_CLIENTS) {
					socket.close();
					continue;
				}

				ClientHandler handler = new ClientHandler(socket);
				handler.colorCode = getRandomColor();
				handler.nickname = "익명" + anonymousCounter++;
				clients.add(handler);

				handler.start();
				broadcastParticipants();
			}

		} catch (IOException e) {
			System.err.println("서버 오류: " + e.getMessage());
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
		boolean firstEntry = true;
		// 클라이언트 목록 복사 후 순회 (동시성 문제 방지 - 더 안전하게 하려면 Collections.synchronizedList 사용 고려)
		List<ClientHandler> currentClients = new ArrayList<>(clients);
		for (ClientHandler client : currentClients) {
			// --- ❗ 닉네임 결정 로직 추가 ❗ ---
			String displayName;
			if (client instanceof GeminiBot) {
				displayName = "Gemini"; // Gemini 봇은 "Gemini"로 표시
			} else if (client.permanentNickname != null && !client.permanentNickname.isEmpty()) {
				displayName = client.permanentNickname; // 실제 닉네임이 있으면 사용
			} else {
				displayName = client.nickname; // 실제 닉네임 없으면 임시 닉네임 사용 (IDENTIFY 전 등)
				// IDENTIFY가 정상적으로 이루어졌다면 이 경우는 거의 발생하지 않아야 함
				System.out.println(
						"[서버 경고] broadcastParticipants: 클라이언트(" + client.nickname + ")의 permanentNickname 없음.");
			}
			String color = client.colorCode;
			// --- 닉네임 결정 로직 끝 ---

			// 첫 항목 아니면 콤마 추가
			if (!firstEntry) {
				sb.append(",");
			}
			// JSON에 displayName 사용 및 escape 처리
			sb.append("{\"nickname\":\"").append(escapeJson(displayName)).append("\",\"color\":\"").append(color)
					.append("\"}");
			firstEntry = false;
		}
		sb.append("]}");
		String messageToSend = sb.toString(); // 최종 메시지
		System.out.println("[서버] 브로드캐스팅 PARTICIPANTS: " + messageToSend); // 전송 내용 로그
		broadcast(messageToSend); // 모든 클라이언트에게 전송
	}

	static List<String> availableColors = new ArrayList<>(
			Arrays.asList("#FF6B6B", "#6BCB77", "#4D96FF", "#FFC75F", "#A66DD4", "#FF9671", "#00C9A7"));

	static String getRandomColor() {
		if (availableColors.isEmpty()) {
			return "#888888"; // fallback (혹은 오류 처리)
		}
		return availableColors.remove(random.nextInt(availableColors.size()));
	}

	static void startNextQuestion() {
		if (questionCount >= MAX_QUESTIONS) {
			broadcast("{\"type\":\"GAME_OVER\",\"message\":\"질문이 모두 완료되었습니다.\"}");
			return;
		}

		do {
			currentQuestion = questionList.get(random.nextInt(questionList.size()));
		} while (usedQuestions.contains(currentQuestion));

		usedQuestions.add(currentQuestion);
		questionCount++;
		resultRevealed = false;

		broadcast("{\"type\":\"QUESTION_PHASE\",\"question\":\"" + currentQuestion + "\"}");
		broadcast("{\"type\":\"chat\",\"color\":\"#888888\",\"message\":\"⏱️ 타이머 시작! 20초 후 답변이 공개됩니다.\"}");

		answers.clear();
		votedUsers.clear();
		voteMap.clear();

		// Gemini 자동 응답
		new Timer().schedule(new TimerTask() {
			public void run() {
				String geminiAnswer = generateGeminiAnswer(currentQuestion);
				answers.put(geminiBot.nickname, geminiAnswer);
				System.out.println("[서버] Gemini 답변 등록: " + geminiAnswer);
				checkAndRevealIfReady(); // Gemini 포함 즉시 공개 가능성 체크
			}
		}, 1000);

		// 20초 후 자동 공개
		new Timer().schedule(new TimerTask() {
			public void run() {
				if (!resultRevealed) {
					revealAnswers();
					resultRevealed = true;
				}
			}
		}, 20000);
	}

	static void revealAnswers() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"type\":\"REVEAL_RESULT\",\"question\":\"").append(currentQuestion).append("\",\"answers\":[");

		List<ClientHandler> shuffledClients = new ArrayList<>(clients);
		Collections.shuffle(shuffledClients);

		int i = 0;
		for (ClientHandler client : shuffledClients) {
			String answer = answers.get(client.nickname);
			if (answer == null)
				answer = "응답 없음";
			answer = answer.replace("\n", " ").replace("\"", "\\\"");
			sb.append("{\"color\":\"").append(client.colorCode).append("\",\"message\":\"").append(answer)
					.append("\"}");
			if (++i < shuffledClients.size())
				sb.append(",");
		}
		sb.append("]}");
		broadcast(sb.toString());

		new Timer().schedule(new TimerTask() {
			public void run() {
				broadcast("{\"type\":\"VOTE_PHASE\"}");
			}
		}, 1000);
	}

	static void checkAndRevealIfReady() {
		if (answers.size() == clients.size() && !resultRevealed) {
			System.out.println("[서버] 모든 답변 제출됨 (하지만 20초 타이머까지 대기)");
		}
		//// 답변이 모두 제출되었지만, 20초가 되기 전이면 기다리기만 함 (아무 것도 안 함)
		// → 타이머가 20초 후에 공개하도록 유도
	}

	static final Map<String, String> colorNameMap = new HashMap<String, String>() {
		{
			put("#FF6B6B", "빨강 유저");
			put("#6BCB77", "초록 유저");
			put("#4D96FF", "파랑 유저");
			put("#FFC75F", "노랑 유저");
			put("#A66DD4", "보라 유저");
			put("#FF9671", "오렌지 유저");
			put("#00C9A7", "청록 유저");
		}
	};

	static String getColorLabel(String colorOrNickname) {
		return colorNameMap.getOrDefault(colorOrNickname, colorOrNickname + " 유저"); // 기본값에 " 유저" 추가 유지
	}

	static void broadcastVoteResult() {
		Map<String, Integer> currentRoundVotes = new HashMap<>(voteMap);
		int maxVotes = 0;
		List<String> topColors = new ArrayList<>();

		for (Map.Entry<String, Integer> entry : currentRoundVotes.entrySet()) {
			int votes = entry.getValue();
			String color = entry.getKey();
			if (votes > maxVotes) {
				maxVotes = votes;
				topColors.clear();
				topColors.add(color);
			} else if (votes == maxVotes && votes > 0) {
				topColors.add(color);
			}
		}

		for (Map.Entry<String, Integer> entry : currentRoundVotes.entrySet()) {
			totalVoteMap.put(entry.getKey(), totalVoteMap.getOrDefault(entry.getKey(), 0) + entry.getValue());
		}

		List<String> namedWinners = new ArrayList<>();
		for (String color : topColors) {
			namedWinners.add(getColorLabel(color));
		}

		String winnerMsg = String.join(", ", namedWinners);
		String broadcastMsg;
		if (namedWinners.isEmpty()) {
			broadcastMsg = "💡 투표 결과: 이번 라운드 투표가 없습니다.";
		} else {
			broadcastMsg = "💡 투표 결과: " + winnerMsg + "가 " + maxVotes + "표를 받았습니다.";
		} // " 유저" 제거

		broadcast("{\"type\":\"chat\",\"color\":\"#000000\",\"message\":\"" + escapeJson(broadcastMsg) + "\"}");

		new Timer().schedule(new TimerTask() {
			public void run() {
				if (questionCount >= MAX_QUESTIONS) {
					broadcastFinalVoteResult();
				} else {
					startNextQuestion();
				}
			}
		}, 3000);
	}

	static Map<String, Integer> totalVoteMap = new HashMap<>();

	// 최종 투표 결과 발표 메서드 수정 (동점자 전원 승리 처리)
	static void broadcastFinalVoteResult() {
		int maxVotes = 0;
		List<String> topVotedColors = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : totalVoteMap.entrySet()) {
			int votes = entry.getValue();
			String targetColor = entry.getKey();
			if (votes > maxVotes) {
				maxVotes = votes;
				topVotedColors.clear();
				topVotedColors.add(targetColor);
			} else if (votes == maxVotes && votes > 0) {
				topVotedColors.add(targetColor);
			}
		}

		// --- 참가자 및 승리자 '실제 닉네임' 리스트 생성 ---
		List<String> participantPermNicknames = new ArrayList<>();
		Map<String, String> colorToPermNicknameMap = new HashMap<>();
		String geminiNickname = null, geminiColor = null;

		for (ClientHandler client : clients) {
			if (client instanceof GeminiBot) {
				geminiNickname = client.nickname;
				geminiColor = client.colorCode;
			} else if (client.permanentNickname != null) {
				participantPermNicknames.add(client.permanentNickname);
				if (client.colorCode != null)
					colorToPermNicknameMap.put(client.colorCode, client.permanentNickname);
			}
		}
		List<String> winnerPermNicknames = new ArrayList<>(); // 최종 승리자 (실제 닉네임 또는 Gemini 닉네임)
		for (String color : topVotedColors) {
			if (color.equals(geminiColor)) {
				if (geminiNickname != null)
					winnerPermNicknames.add(geminiNickname);
			} else {
				String permNickname = colorToPermNicknameMap.get(color);
				if (permNickname != null)
					winnerPermNicknames.add(permNickname);
			}
		}
		// --- 리스트 생성 끝 ---

		// 최종 결과 메시지 생성 (화면 표시용)
		List<String> winnerLabels = new ArrayList<>();
		for (String nickOrColor : winnerPermNicknames) {
			winnerLabels.add(getColorLabel(nickOrColor));
		}
		String winnerMsg = String.join(", ", winnerLabels);
		StringBuilder message = new StringBuilder("🏁 최종 투표 결과: ");
		if (winnerLabels.isEmpty())
			message.append("승자가 없습니다.");
		else
			message.append(winnerMsg).append(" (이)가 총 ").append(maxVotes).append("표를 받아 승리했습니다.");

		// --- ❗ JSON에 winners와 participants 목록 추가 ❗ ---
		StringBuilder jsonResult = new StringBuilder("{");
		jsonResult.append("\"type\":\"FINAL_RESULT\",");
		jsonResult.append("\"message\":\"").append(escapeJson(message.toString())).append("\",");
		// 승리자 목록 (JSON 배열 형태)
		jsonResult.append("\"winners\":[");
		for (int i = 0; i < winnerPermNicknames.size(); i++) {
			jsonResult.append("\"").append(escapeJson(winnerPermNicknames.get(i))).append("\"");
			if (i < winnerPermNicknames.size() - 1)
				jsonResult.append(",");
		}
		jsonResult.append("],");
		// 참가자 목록 (JSON 배열 형태)
		jsonResult.append("\"participants\":[");
		for (int i = 0; i < participantPermNicknames.size(); i++) {
			jsonResult.append("\"").append(escapeJson(participantPermNicknames.get(i))).append("\"");
			if (i < participantPermNicknames.size() - 1)
				jsonResult.append(",");
		}
		jsonResult.append("]}");
		// --- JSON 수정 끝 ---

		broadcast(jsonResult.toString()); // 수정된 JSON 브로드캐스트
		System.out.println("[서버] FINAL_RESULT 브로드캐스트: " + jsonResult.toString()); // 전송 내용 로그

		// 점수 업데이트 로직 호출 (실제 닉네임 리스트 전달)
		updateScores(winnerPermNicknames, participantPermNicknames); // <<--- 파라미터 변경됨!

	}

	// --- ❗ updateScores 메소드 시그니처 변경 ❗ ---
	static void updateScores(List<String> winnerPermNicknames, List<String> participantPermNicknames) {
		System.out.println("[서버] 점수 업데이트 호출 준비 (Permanent Nicknames):");
		System.out.println("  승리자 실제 닉네임 목록: " + winnerPermNicknames);
		System.out.println("  참가자 실제 닉네임 목록: " + participantPermNicknames);
		try {
			// ❗ DatabaseManager.updateScoresAfterGame 시그니처도 확인 필요 ❗
			// 만약 DB 메소드가 (List<String> winners, List<String> participants) 형태라면 그대로 호출
			DB.DatabaseManager.updateScoresAfterGame(winnerPermNicknames, participantPermNicknames);
			System.out.println("[서버] DatabaseManager.updateScoresAfterGame 호출 완료.");
		} catch (Exception e) {
			System.err.println("[서버 오류] 점수 반영 중 오류 발생: " + e.getMessage());
			e.printStackTrace(); // DB 오류는 여기서 출력됨
		}
	}

	// JSON 문자열 내 특수문자 escape 처리 유틸리티 메소드 (필요시 추가)
	private static String escapeJson(String str) {
		if (str == null)
			return null;
		return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t",
				"\\t");
	}

	static String generateGeminiAnswer(String question) {
		try {
			// Gemini에게 짧게 답변하라고 요청하는 프롬프트 추가
			String prompt = question + "\n\n위 질문에 대해 두 문장 이내로 간단하고 자연스럽게 대답해줘. 예를 들면 대화체처럼 말해줘.";
			String answer = ConGemini.getResponse(prompt);

			// 너무 긴 답변은 300자 이내로 자르기 (예외 방지용)
			if (answer.length() > 300) {
				answer = answer.substring(0, 300) + "...";
			}

			return answer;

		} catch (IOException e) {
			e.printStackTrace();
			return "Gemini 응답 실패: " + e.getMessage();
		}
	}

	static class ClientHandler extends Thread {
		Socket socket;
		BufferedReader br;
		BufferedWriter bw;
		String nickname; // 서버가 부여한 임시 닉네임 (예: "익명1")
		String colorCode;
		String permanentNickname = null; // <<--- 클라이언트의 실제 닉네임 저장용 변수 추가
		boolean isReady = false;

		// 생성자 (UTF-8 인코딩 명시 확인)
		public ClientHandler(Socket socket) {
			this.socket = socket;
			if (socket != null) {
				try {
					br = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
					bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
				} catch (IOException e) {
					System.err.println("[서버 오류] ClientHandler 스트림 생성 실패: " + e.getMessage());
					e.printStackTrace();
					try {
						if (socket != null && !socket.isClosed())
							socket.close();
					} catch (IOException ioex) {
					}
				}
			}
		}

		// run 메소드 (IDENTIFY 메시지 처리 로직 추가됨)
		@Override
		public void run() {
			try {
				// 클라이언트에게 임시 닉네임으로 INIT 메시지 전송
				send("{\"type\":\"INIT\",\"nickname\":\"" + nickname + "\",\"color\":\"" + colorCode + "\"}");

				String msg;
				while ((msg = br.readLine()) != null) {
					System.out.println("[서버 수신 " + nickname + "] " + msg); // 수신 로그

					// 시작 메시지 처리
					if (msg.contains("\"type\":\"start\"")) {
						System.out.println("[서버 " + nickname + "] Start 메시지 감지됨!");
						isReady = true;
						readyCount++;
						// 실제 플레이어 수 계산 (Gemini 제외)
						int realPlayers = 0;
						for (ClientHandler c : clients) {
							if (!(c instanceof GeminiBot))
								realPlayers++;
						}
						System.out.println("[서버] 준비 상태: " + readyCount + "/" + realPlayers);
						if (readyCount == realPlayers && realPlayers >= 2) {
							System.out.println("[서버] 게임 시작 조건 충족! GAME_START 브로드캐스팅 시도...");
							broadcast("{\"type\":\"GAME_START\"}");
							System.out.println("[서버] GAME_START 브로드캐스팅 완료.");
							startNextQuestion(); // 첫 질문 시작
						} else {
							System.out.println("[서버] 아직 게임 시작 조건 미충족.");
						}
					}
					// 답변 제출 메시지 처리
					else if (msg.contains("\"type\":\"ANSWER_SUBMIT\"")) {
						String answer = extractValue(msg, "message");
						System.out.println("[서버] " + nickname + " 의 답변 수신: " + answer);
						answers.put(nickname, answer); // 임시 닉네임 기준으로 답변 저장
						checkAndRevealIfReady();
					}
					// 투표 메시지 처리
					else if (msg.contains("\"type\":\"vote\"")) {
						String targetColor = extractValue(msg, "target");
						voteMap.put(targetColor, voteMap.getOrDefault(targetColor, 0) + 1);
						votedUsers.add(nickname); // 투표한 사람 (임시 닉네임 기준)
						// 실제 플레이어 수 계산
						int realPlayers = 0;
						for (ClientHandler c : clients) {
							if (!(c instanceof GeminiBot))
								realPlayers++;
						}
						// 모든 실제 플레이어가 투표했으면 결과 발표
						if (votedUsers.size() == realPlayers) {
							broadcastVoteResult();
						}
					}
					// --- ❗ IDENTIFY 메시지 처리 추가 ❗ ---
					else if (msg.contains("\"type\":\"IDENTIFY\"")) {
						// 클라이언트가 보낸 실제 닉네임(permanentNickname) 저장
						this.permanentNickname = extractValue(msg, "permNickname");
						System.out.println(
								"[서버] 클라이언트 식별됨: " + this.nickname + " -> 실제 닉네임 '" + this.permanentNickname + "'");
					}
					// --- IDENTIFY 처리 끝 ---
					// 일반 채팅 메시지 처리
					else if (!msg.trim().startsWith("{")) {
						String escapedMsg = escapeJson(msg); // 채팅 내용도 escape
						String chatJson = "{\"type\":\"chat\",\"color\":\"" + colorCode + "\",\"message\":\""
								+ escapedMsg + "\"}";
						broadcast(chatJson);
					}
					// 클라이언트 종료 요청 처리 (선택적)
					else if (msg.contains("\"type\":\"quit\"")) {
						System.out.println("[서버] " + nickname + " 클라이언트 종료 요청 수신.");
						break; // while 루프 종료 -> finally 블록 실행됨
					}

				} // end of while
			} catch (SocketTimeoutException e) {
				System.err.println("소켓 타임아웃 (" + nickname + "): " + e.getMessage());
			} catch (IOException e) {
				if (e.getMessage() != null && e.getMessage().toLowerCase().contains("connection reset")) {
					System.err.println("클라이언트 연결 리셋됨 (" + nickname + ").");
				} else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("socket closed")) {
					System.err.println("소켓 닫힘 (" + nickname + ").");
				} else {
					System.err.println("네트워크 오류 (" + nickname + "): " + e.getMessage());
				}
			} catch (Exception e) { // 그 외 예외 처리
				System.err.println("[서버 오류 " + nickname + "] run 메소드 오류: " + e.getMessage());
				e.printStackTrace();
			} finally {
				System.out
						.println("[서버] 클라이언트 핸들러 종료 시작: " + (permanentNickname != null ? permanentNickname : nickname));
				// ... (기존 finally 블록의 자원 정리 코드 동일) ...
				try {
					boolean removed = clients.remove(this);
					System.out.println("[서버] 클라이언트 제거 " + (removed ? "성공" : "실패") + ": "
							+ (permanentNickname != null ? permanentNickname : nickname));
					if (colorCode != null && !availableColors.contains(colorCode)) {
						availableColors.add(colorCode);
						System.out.println("[서버] 색상 반납: " + colorCode);
					}
					if (isReady) {
						readyCount = Math.max(0, readyCount - 1);
					}
					broadcastParticipants();
					votedUsers.remove(nickname);
					answers.remove(nickname);
					if (br != null)
						try {
							br.close();
						} catch (IOException e) {
							/* ignore */ }
					if (bw != null)
						try {
							bw.close();
						} catch (IOException e) {
							/* ignore */ }
					if (socket != null && !socket.isClosed())
						try {
							socket.close();
						} catch (IOException e) {
							/* ignore */ }
					System.out.println(
							"[서버] 클라이언트 리소스 정리 완료: " + (permanentNickname != null ? permanentNickname : nickname));
				} catch (Exception e) {
					System.err.println("[서버] 클라이언트 핸들러 finally 블록 오류: " + e.getMessage());
					e.printStackTrace();
				}
			}
		} // end of run

		// send 메소드 (변경 없음)
		void send(String msg) throws IOException {
			if (bw != null) {
				bw.write(msg);
				bw.newLine();
				bw.flush();
			} else {
				throw new IOException("BufferedWriter is null.");
			}
		}
	} // --- ClientHandler 클래스 끝 ---

	static class GeminiBot extends ClientHandler {
		public GeminiBot(String nickname, String colorCode) {
			super(null);
			this.nickname = nickname;
			this.colorCode = colorCode;
		}

		@Override
		public void run() {
		}

		@Override
		void send(String msg) {
		}
	}

	static String extractValue(String json, String key) {
		int idx = json.indexOf(key);
		if (idx == -1)
			return "";
		int start = json.indexOf("\"", idx + key.length() + 2);
		int end = json.indexOf("\"", start + 1);
		return json.substring(start + 1, end);
	}

	// 새로운 라운드를 위한 타이머 설정
	static void startNewRoundTimer() {
		new Timer().schedule(new TimerTask() {
			public void run() {
				// 20초 타이머 후, 자동으로 답변 공개
				if (!resultRevealed) {
					revealAnswers();
					resultRevealed = true;
				}
			}
		}, 20000);
	}

}
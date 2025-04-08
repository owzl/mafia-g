package MafiaG;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Timer;
import java.util.TimerTask;

public class PlayUITest extends JFrame {
    private JPanel tutorialPanel, chatPanel, inputPanel;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton startButton;
    private DefaultListModel<String> participantModel;

    public PlayUITest() {
        setTitle("MafiaG");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 상단 헤더
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(238, 238, 238));
        header.setBorder(new EmptyBorder(10, 20, 10, 20));
        header.add(new JLabel(new ImageIcon("src/images/MafiaG_wordlogo.jpg")), BorderLayout.WEST);
        header.add(new JLabel("닉네임 님 환영합니다", SwingConstants.RIGHT), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // 메인 영역
        JPanel mainPanel = new JPanel(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        // 사이드바
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBackground(new Color(240, 234, 255));
        
        // 랭킹 영역
        JPanel rankingPanel = new JPanel();
        rankingPanel.setLayout(new BoxLayout(rankingPanel, BoxLayout.Y_AXIS));
        rankingPanel.setBackground(Color.WHITE);
        rankingPanel.setBorder(BorderFactory.createTitledBorder("랭킹 내역"));
        rankingPanel.add(new JLabel("1위: user3 (300점)"));
        rankingPanel.add(new JLabel("2위: user1 (200점)"));
        rankingPanel.add(new JLabel("3위: user2 (111점)"));

        // 참여자 영역
        participantModel = new DefaultListModel<>();
        JList<String> participantList = new JList<>(participantModel);
        participantList.setBorder(BorderFactory.createTitledBorder("참여자 명단"));
        JScrollPane participantScroll = new JScrollPane(participantList);

        // Start 버튼
        startButton = new JButton("Start");
        startButton.setPreferredSize(new Dimension(200, 50));
        startButton.addActionListener(e -> onStart());

        JPanel sidebarContent = new JPanel(new BorderLayout());
        sidebarContent.add(rankingPanel, BorderLayout.NORTH);
        sidebarContent.add(participantScroll, BorderLayout.CENTER);

        sidebar.add(sidebarContent, BorderLayout.CENTER);
        sidebar.add(startButton, BorderLayout.SOUTH);

        mainPanel.add(sidebar, BorderLayout.WEST);

        // 채팅 및 튜토리얼 영역
        JPanel chatContainer = new JPanel(new BorderLayout());
        mainPanel.add(chatContainer, BorderLayout.CENTER);

        // 튜토리얼 패널
        tutorialPanel = new JPanel();
        tutorialPanel.setBackground(new Color(240, 240, 250));
        tutorialPanel.setLayout(new GridBagLayout());
        JLabel tutorialText = new JLabel("게임 시작 전 튜토리얼 이미지 또는 설명");
        tutorialText.setFont(tutorialText.getFont().deriveFont(24f));
        tutorialPanel.add(tutorialText);
        chatContainer.add(tutorialPanel, BorderLayout.CENTER);

        // 채팅 패널
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBackground(new Color(248, 248, 255));
        chatPanel.setVisible(false);

        JScrollPane scrollPane = new JScrollPane(chatPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);  // 스크롤 부드럽게
        scrollPane.setBorder(null);  // 불필요한 테두리 제거

        chatContainer.add(scrollPane, BorderLayout.CENTER);


        // 입력창
        inputPanel = new JPanel(new BorderLayout());
        inputPanel.setPreferredSize(new Dimension(0, 50));
        chatInput = new JTextField();
        chatInput.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        chatInput.setEnabled(false);
        chatInput.addActionListener(e -> {
            String msg = chatInput.getText().trim();
            if (!msg.isEmpty()) {
                addMessage("나", msg, true);
                chatInput.setText("");
            }
        });
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.setVisible(false);
        chatContainer.add(inputPanel, BorderLayout.SOUTH);

        // 예시 참여자
        for (String name : new String[]{"user1", "user2", "user3"}) {
            participantModel.addElement(name);
        }
    }

    private void onStart() {
        startButton.setEnabled(false);
        startButton.setText("대기 중...");

        Timer timer = new Timer();
        final int[] count = {5};

        JLabel tutorialLabel = (JLabel) tutorialPanel.getComponent(0);

        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (count[0] > 0) {
                    tutorialLabel.setText("게임 시작까지 " + count[0]-- + "초...");
                } else {
                    timer.cancel();
                    tutorialPanel.setVisible(false);
                    chatPanel.setVisible(true);
                    inputPanel.setVisible(true);
                    chatInput.setEnabled(true);
                    chatInput.requestFocus();
                    // 예시 메시지
//                    new Timer().schedule(new TimerTask() {
//                        public void run() {
//                            addMessage("user2", "안녕? 나는 마피아가 아니야.", false);
//                            addMessage("user3", "안녕? 나는 마피아가 아니야.", false);
//                            addMessage("user4", "안녕? 나는 마피아가 아니야.", false);
//                        }
//                    }, 1000);
                }
            }
        }, 0, 1000);
    }

   
    private void addMessage(String nickname, String msg, boolean isMine) {
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.X_AXIS));
        messagePanel.setOpaque(false);
        messagePanel.setBorder(new EmptyBorder(0, 0, 10, 0));  // 말풍선 간 간격 추가

        // 말풍선 텍스트 (고정 폭 유지, 최소 폭도 지정)
        JLabel message = new JLabel("<html><div style='width:330px'>" + msg + "</div></html>");
        message.setBorder(new EmptyBorder(10, 10, 10, 10));
        message.setOpaque(true);

        // 세로 길이 고정 (높이 고정 50으로 설정)
        int fixedHeight = 50;
        message.setPreferredSize(new Dimension(450, fixedHeight));  // 세로 길이 고정
        message.setMaximumSize(new Dimension(450, fixedHeight));  // 세로 길이 고정

        // 배경색 설정
        if (isMine) {
            message.setBackground(new Color(224, 224, 224));  // 내 말풍선 색
            messagePanel.add(Box.createHorizontalGlue());     // 오른쪽 정렬
            messagePanel.add(message);
        } else {
            Color[] colors = {
                new Color(255, 221, 221), new Color(255, 229, 204),
                new Color(255, 255, 204), new Color(221, 255, 221),
                new Color(221, 238, 255)
            };
            message.setBackground(colors[(int)(Math.random() * colors.length)]); // 랜덤 색
            messagePanel.add(message);
            messagePanel.add(Box.createHorizontalGlue()); // 왼쪽 정렬
        }

        messagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        chatPanel.add(messagePanel);
        chatPanel.revalidate();
        chatPanel.repaint();

        JScrollPane scrollPane = (JScrollPane) chatPanel.getParent().getParent();
        JScrollBar vBar = scrollPane.getVerticalScrollBar();
        SwingUtilities.invokeLater(() -> vBar.setValue(vBar.getMaximum()));
    }

    // 랜덤 배경색 추출 함수
    private Color getRandomColor() {
        Color[] colors = {
            new Color(255, 204, 204),
            new Color(255, 229, 204),
            new Color(255, 255, 204),
            new Color(204, 255, 204),
            new Color(204, 229, 255)
        };
        return colors[(int) (Math.random() * colors.length)];
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PlayUITest().setVisible(true));
    }
}

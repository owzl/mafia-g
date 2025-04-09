package MafiaG;

import java.nio.charset.StandardCharsets;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class PlayUI extends JFrame implements ActionListener {
	static Socket sock;
	static BufferedWriter bw = null;
	static BufferedReader br = null;

	private DefaultListModel<String> participantModel;
	// private JTextArea rankingArea;
	private RankingPanel rankingPanel;
	private JTextField chatInput;
	private JTextPane chatPane;
	private StyledDocument doc;
	private JButton startButton;
	private JComboBox<String> voteChoice;
	private JButton voteBtn;
	private JLabel timerLabel;
	
	// 튜토리얼 관련
	private JPanel chatContainerCards; // CardLayout이 적용된 패널
	private CardLayout cardLayout;

	private String myColor = "";
	private boolean gameStarted = false;

//	private final Map<String, String> colorToNameMap = Map.of("#FF6B6B", "빨강 유저", "#6BCB77", "초록 유저", "#4D96FF",
//			"파랑 유저", "#FFC75F", "노랑 유저", "#A66DD4", "보라 유저", "#FF9671", "오렌지 유저", "#00C9A7", "청록 유저");
	private final Map<String, String> colorToNameMap = new HashMap<String, String>() {{
	    put("#FF6B6B", "빨강 유저");
	    put("#6BCB77", "초록 유저");
	    put("#4D96FF", "파랑 유저");
	    put("#FFC75F", "노랑 유저");
	    put("#A66DD4", "보라 유저");
	    put("#FF9671", "오렌지 유저");
	    put("#00C9A7", "청록 유저");
	}};
	private final Map<String, String> nameToColorMap = new HashMap<>();
	

	public PlayUI() {
		setTitle("MafiaG");
		setSize(1200, 800);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setLayout(new BorderLayout());
		setupUI();
		connectToServer();
		setLocationRelativeTo(null);

		 addWindowListener(new WindowAdapter() {
	            @Override
	            public void windowClosing(WindowEvent e) {
	                int result = JOptionPane.showConfirmDialog(
	                    PlayUI.this,
	                    "정말 종료하시겠습니까?",
	                    "종료 확인",
	                    JOptionPane.YES_NO_OPTION
	                );
	                
	                System.out.println("종료 요청 확인됨");
	                
	                if (result == JOptionPane.YES_OPTION) {
	                	System.out.println("정리 작업 시작");
	                	
	                 // 네트워크 자원 정리
	                    closeConnection();
	                    System.out.println("연결 종료");
	                    sendToServer("{\"type\":\"quit\"}");
	                    try {
	                        Thread.sleep(1000); // 자원 해제 대기
	                    } catch (InterruptedException e1) {
	                        e1.printStackTrace();
	                    }
	                    
	                 // 창 종료
	                    dispose();
	                    
	                 // 모든 스레드 정리 후 강제 종료
	                    System.exit(0);
	                    System.out.println("완전 종료");
	                    
	                }
	            }
	        });
	        setLocationRelativeTo(null);
	    }

	private void setupUI() {
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(new Color(238, 238, 238));
		header.setBorder(new EmptyBorder(10, 20, 10, 20));
		header.add(new JLabel("익명 마피아 게임", SwingConstants.LEFT), BorderLayout.WEST);
		add(header, BorderLayout.NORTH);

		JPanel mainPanel = new JPanel(new BorderLayout());
		add(mainPanel, BorderLayout.CENTER);
		

		JPanel sidebar = new JPanel(new BorderLayout());
		sidebar.setPreferredSize(new Dimension(200, 0));
		sidebar.setBackground(new Color(240, 234, 255));

		// 랭킹창
		JPanel sidebarContent = new JPanel(new BorderLayout());  // 선언 먼저
//		rankingArea = new JTextArea("랭킹\n", 5, 20);
//		rankingArea.setEditable(false);
//		JScrollPane rankingScroll = new JScrollPane(rankingArea);
//		rankingScroll.setBorder(BorderFactory.createTitledBorder("랭킹"));
		rankingPanel = new RankingPanel();
		sidebarContent.add(rankingPanel, BorderLayout.NORTH);

		// 참여자 명단
		participantModel = new DefaultListModel<>();
		JList<String> participantList = new JList<>(participantModel);
		JScrollPane participantScroll = new JScrollPane(participantList);
		participantScroll.setBorder(BorderFactory.createTitledBorder("참여자 명단"));

		startButton = new JButton("Start");
		startButton.setEnabled(true);
		startButton.setPreferredSize(new Dimension(200, 50));
		startButton.addActionListener(e -> {
			startButton.setEnabled(false);
			sendToServer("{\"type\":\"start\"}");
		});

//		JPanel sidebarContent = new JPanel(new BorderLayout());
//		sidebarContent.add(rankingScroll, BorderLayout.NORTH);
		sidebarContent.add(participantScroll, BorderLayout.CENTER);
		sidebar.add(sidebarContent, BorderLayout.CENTER);
		sidebar.add(startButton, BorderLayout.SOUTH);
		mainPanel.add(sidebar, BorderLayout.WEST);

		// 채팅창
		// CardLayout을 위한 컨테이너
		cardLayout = new CardLayout();
		chatContainerCards = new JPanel(cardLayout);

		// 튜토리얼 이미지
		ImageIcon tutorialImage = new ImageIcon("src/img/TutorialSample.png"); // 파일 경로 맞춰서 수정
		JLabel tutorialLabel = new JLabel(tutorialImage);
		tutorialLabel.setHorizontalAlignment(JLabel.CENTER);
		JPanel tutorialPanel = new JPanel(new BorderLayout());
		tutorialPanel.add(tutorialLabel, BorderLayout.CENTER);

		
		JPanel chatContainer = new JPanel(new BorderLayout());
		chatPane = new JTextPane();
		chatPane.setEditable(false);
		doc = chatPane.getStyledDocument();
		JScrollPane chatScroll = new JScrollPane(chatPane);
		chatContainer.add(chatScroll, BorderLayout.CENTER);
		
		// 기존 채팅창 패널을 카드에 추가
		chatContainerCards.add(tutorialPanel, "tutorial");
		chatContainerCards.add(chatContainer, "chat");

		// 채팅 입력창 input text
		JPanel inputPanel = new JPanel(new BorderLayout());
		chatInput = new JTextField();
		chatInput.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
		chatInput.setEnabled(false);
		chatInput.setBackground(Color.LIGHT_GRAY);
		chatInput.addActionListener(this);
		inputPanel.add(chatInput, BorderLayout.CENTER);
		chatContainer.add(inputPanel, BorderLayout.SOUTH);
//		mainPanel.add(chatContainer, BorderLayout.CENTER); // 튜토리얼 이미지 반영 전
		mainPanel.add(chatContainerCards, BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		voteChoice = new JComboBox<>();
		voteChoice.setPreferredSize(new Dimension(150, 30));
		voteChoice.setEnabled(false);

		voteBtn = new JButton("투표");
		voteBtn.setEnabled(false);
		voteBtn.addActionListener(e -> {
			String selectedLabel = (String) voteChoice.getSelectedItem();
			if (selectedLabel != null) {
				String selectedColor = nameToColorMap.get(selectedLabel);
				if (selectedColor != null) {
					sendToServer("{\"type\":\"vote\",\"target\":\"" + selectedColor + "\"}");
					voteChoice.setEnabled(false);
					voteBtn.setEnabled(false);
				}
			}
		});

		timerLabel = new JLabel("남은 시간: 20초");
		bottomPanel.add(new JLabel("투표 대상:"));
		bottomPanel.add(voteChoice);
		bottomPanel.add(voteBtn);
		bottomPanel.add(timerLabel);
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);
	}

	public void actionPerformed(ActionEvent e) {
		String msg = chatInput.getText().trim();
		if (!msg.isEmpty()) {
			sendToServer("{\"type\":\"ANSWER_SUBMIT\",\"message\":\"" + msg + "\"}");
			appendAnonymousChat(myColor, msg); // ✅ 내 답변도 미리 출력
			chatInput.setText("");
		}
	}

	private void appendAnonymousChat(String colorCode, String msg) {
		SimpleAttributeSet attr = new SimpleAttributeSet();
		try {
			StyleConstants.setForeground(attr, Color.decode(colorCode));
		} catch (NumberFormatException e) {
			StyleConstants.setForeground(attr, Color.GRAY);
		}
		StyleConstants.setFontSize(attr, 16);
		try {
			doc.insertString(doc.getLength(), msg + "\n", attr);
			doc.setParagraphAttributes(doc.getLength(), 1, attr, false);
			chatPane.setCaretPosition(doc.getLength());
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
	}

	private void connectToServer() {
		try {
			sock = new Socket("172.30.1.66", 3579);
			br = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
			bw = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));

			Thread serverThread = new Thread(() -> {
				String line;
				try {
					while ((line = br.readLine()) != null) {
						String finalLine = line;
						System.out.println("서버로부터: " + finalLine);

						if (finalLine.contains("\"type\":\"INIT\"")) {
							myColor = extractValue(finalLine, "color");
						} else if (finalLine.contains("\"type\":\"QUESTION_PHASE\"")) {
							String question = extractValue(finalLine, "question");
							SwingUtilities.invokeLater(() -> {
								appendAnonymousChat("#444444", "❓ 질문: " + question);
								// 타이머 시작
								new Thread(() -> {
									for (int i = 20; i >= 0; i--) {
										int sec = i;
										SwingUtilities.invokeLater(() -> timerLabel.setText("남은 시간: " + sec + "초"));
										try {
											Thread.sleep(1000);
										} catch (InterruptedException ex) {
											break;
										}
									}
								}).start();
							});
						} else if (finalLine.contains("\"type\":\"chat\"")) {
							String color = extractValue(finalLine, "color");
							String msg = extractValue(finalLine, "message");
							appendAnonymousChat(color, msg);
						} else if (finalLine.contains("\"type\":\"REVEAL_RESULT\"")) {
							appendAnonymousChat("#444444", "💬 답변 공개 완료!");

							//  JSON 파싱 (answers)
							int start = finalLine.indexOf("\"answers\":[") + 11;
							int end = finalLine.lastIndexOf("]}");
							if (start != -1 && end != -1 && end > start) {
								String answerData = finalLine.substring(start, end);
								String[] items = answerData.split("\\},\\{");

								for (String item : items) {
									String color = extractValue("{" + item + "}", "color");
									String message = extractValue("{" + item + "}", "message");
									appendAnonymousChat(color, "💬 " + message);
								}
							}
						} else if (finalLine.contains("\"type\":\"PARTICIPANTS\"")) {
							SwingUtilities.invokeLater(() -> {
								voteChoice.removeAllItems();
								nameToColorMap.clear();
								participantModel.clear();
								String[] entries = finalLine.split("\\{");
								for (String entry : entries) {
									if (entry.contains("\"color\"")) {
										String color = extractValue(entry, "color");
										String label = colorToNameMap.getOrDefault(color, color + " 유저");
										voteChoice.addItem(label);
										nameToColorMap.put(label, color);
										participantModel.addElement(label);
									}
								}
							});
						} else if (finalLine.contains("\"type\":\"VOTE_PHASE\"")) {
							SwingUtilities.invokeLater(() -> {
								voteChoice.setEnabled(true);
								voteBtn.setEnabled(true);
							});
						} else if (finalLine.contains("\"type\":\"GAME_START\"")) {
							SwingUtilities.invokeLater(() -> {
								gameStarted = true;
								chatInput.setEnabled(true);
								chatInput.setBackground(Color.WHITE);
								startButton.setEnabled(false);
								
								// 카드 전환: 튜토리얼 → 채팅창
						        cardLayout.show(chatContainerCards, "chat");
							});
						}
					}
				} catch (IOException e) {
					System.out.println("서버 연결 종료됨");
					closeConnection();
				}
			});
			serverThread.setDaemon(true);
			serverThread.start();

		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "서버 연결 실패", "오류", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void sendToServer(String message) {
		try {
			if (bw != null) {
				bw.write(message + "\n");
				bw.flush();
			}
		} catch (IOException ex) {
			System.out.println("서버로 메시지 전송 실패");
			closeConnection();
		}
	}

	private void closeConnection() {
		try {
     	    if (sock != null && !sock.isClosed()) {
     	        sock.shutdownInput();  // 👈 먼저 입력 스트림 닫기
     	        sock.shutdownOutput(); // 👈 출력도 명시적으로 종료
     	    }
     	} catch (IOException e) {
     	    e.printStackTrace();
     	    System.err.println("sock 닫기 실패: " + e.getMessage());
     	}
     	
     	    try {
     	        if (br != null) {
     	            br.close();
     	        }
     	    } catch (IOException e) {
     	        System.err.println("br 닫기 실패: " + e.getMessage());
     	    }
 
     	    try {
     	        if (bw != null) {
     	            bw.close();
     	        }
     	    } catch (IOException e) {
     	        System.err.println("bw 닫기 실패: " + e.getMessage());
     	    }
	}

	private String extractValue(String json, String key) {
		try {
			String pattern = "\"" + key + "\":\"";
			int start = json.indexOf(pattern) + pattern.length();
			int end = json.indexOf("\"", start);
			return json.substring(start, end);
		} catch (Exception e) {
			return "";
		}
	}
}
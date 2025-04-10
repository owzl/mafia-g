package MafiaG;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets; // <<--- StandardCharsets import 추가
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class PlayUI extends JFrame implements ActionListener {
	static Socket sock;
	static BufferedWriter bw = null;
	static BufferedReader br = null;

	// --- 기존 멤버 변수 ---
	private DefaultListModel<String> participantModel;
	private RankingPanel rankingPanel;
	private JTextField chatInput;
	private JTextPane chatPane;
	private StyledDocument doc;
	private JButton startButton;
	private JComboBox<String> voteChoice;
	private JButton voteBtn;
	private JLabel timerLabel;
	private JPanel chatContainerCards;
	private CardLayout cardLayout;
	private String myColor = "";
	private boolean gameStarted = false;
	private String temporaryNickname = ""; // 서버가 부여한 임시 닉네임 (선택적 저장)

	// --- ❗ 멤버 변수 추가 ❗ ---
	private String permanentNickname; // 로그인 시 받은 실제 닉네임 저장용

	private final Map<String, String> colorToNameMap = new HashMap<String, String>() {
		{
			// ... (기존 내용 동일) ...
		}
	};
	private final Map<String, String> nameToColorMap = new HashMap<>();

	// --- ❗ 생성자 수정: 실제 닉네임(permNick)을 받도록 변경 ❗ ---
	public PlayUI(String permNick) { // 생성자 파라미터 추가
		this.permanentNickname = permNick; // 전달받은 실제 닉네임 저장
		System.out.println("[PlayUI 생성] Permanent Nickname 설정됨: " + this.permanentNickname); // 로그 추가

		setTitle("MafiaG");
		setSize(1200, 800);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setLayout(new BorderLayout());

		// --- 헤더 설정 (로고 추가 등 - 사용자 최신 코드 반영) ---
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(new Color(238, 238, 238));
		header.setBorder(new EmptyBorder(10, 20, 10, 20));
		ImageIcon icon = new ImageIcon("src/img/logo.png");
		JLabel logoLabel = new JLabel(icon);
		header.add(logoLabel, BorderLayout.WEST);
		JLabel titleLabel = new JLabel("MafiaG", SwingConstants.LEFT);
		titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
		header.add(titleLabel, BorderLayout.CENTER);
		add(header, BorderLayout.NORTH);
		// --- 헤더 설정 끝 ---

		setupUI(); // UI 컴포넌트 설정
		connectToServer(); // 서버 연결 (생성자 마지막에 호출)
		setLocationRelativeTo(null);

		// --- 윈도우 닫기 리스너 (기존 코드 동일) ---
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// ... (기존 닫기 확인 로직 동일) ...
			}
		});
	} // --- 생성자 끝 ---

	private void setupUI() {
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(new Color(238, 238, 238));
		header.setBorder(new EmptyBorder(10, 20, 10, 20));
		header.add(new JLabel("MafiaG", SwingConstants.LEFT), BorderLayout.WEST);
		add(header, BorderLayout.NORTH);

		JPanel mainPanel = new JPanel(new BorderLayout());
		add(mainPanel, BorderLayout.CENTER);

		JPanel sidebar = new JPanel(new BorderLayout());
		sidebar.setPreferredSize(new Dimension(200, 0));
		sidebar.setBackground(new Color(240, 234, 255));

		// 랭킹창
		JPanel sidebarContent = new JPanel(new BorderLayout()); // 선언 먼저
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
			// ❗ 서버 주소 확인 필요! (localhost 또는 실제 서버 IP)
			sock = new Socket("172.30.1.47", 3579);
			// ❗ UTF-8 인코딩 명시 확인
			br = new BufferedReader(new InputStreamReader(sock.getInputStream(), StandardCharsets.UTF_8));
			bw = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream(), StandardCharsets.UTF_8));

			// 서버 메시지 수신 스레드
			Thread serverThread = new Thread(() -> {
				String line;
				try {
					while ((line = br.readLine()) != null) {
						String finalLine = line; // effectively final for lambda
						System.out.println("서버로부터: " + finalLine); // 수신 메시지 로그

						// === 메시지 타입별 처리 ===

						// 1. 초기화 (INIT) 메시지 처리
						if (finalLine.contains("\"type\":\"INIT\"")) {
							myColor = extractValue(finalLine, "color");
							temporaryNickname = extractValue(finalLine, "nickname"); // 임시 닉네임 저장
							System.out.println("[클라이언트] INIT 수신: TempNick=" + temporaryNickname + ", Color=" + myColor);

							// --- ❗ IDENTIFY 메시지 전송 추가 ❗ ---
							if (this.permanentNickname != null && !this.permanentNickname.isEmpty()) {
								// permanentNickname에 포함될 수 있는 특수문자(따옴표 등) escape 처리
								String escapedNickname = escapeJson(this.permanentNickname);
								String identifyMsg = "{\"type\":\"IDENTIFY\",\"permNickname\":\"" + escapedNickname
										+ "\"}";
								sendToServer(identifyMsg); // 서버로 IDENTIFY 메시지 전송
								System.out.println("[클라이언트] IDENTIFY 전송: " + identifyMsg);
							} else {
								System.err.println("[클라이언트 경고] Permanent nickname이 없어 IDENTIFY 메시지를 보낼 수 없습니다.");
							}
							// --- IDENTIFY 메시지 전송 끝 ---

						}
						// 2. 참가자 목록 (PARTICIPANTS) 메시지 처리
						else if (finalLine.contains("\"type\":\"PARTICIPANTS\"")) {
							SwingUtilities.invokeLater(() -> {
								// ... (기존 PARTICIPANTS 처리 로직 동일 - 자기 자신 투표 제외 포함) ...
								System.out.println("[클라이언트] 참가자 목록 수신. 내 색상: " + myColor);
								voteChoice.removeAllItems();
								nameToColorMap.clear();
								participantModel.clear();
								try {
									int listStartIndex = finalLine.indexOf("\"list\":[") + "\"list\":[".length();
									int listEndIndex = finalLine.lastIndexOf("]");
									if (listEndIndex > listStartIndex) {
										String listData = finalLine.substring(listStartIndex, listEndIndex);
										String[] entries = listData.split("\\}(?=\\s*,\\s*\\{)");
										for (String entry : entries) {
											String currentEntry = entry.trim();
											if (!currentEntry.startsWith("{"))
												currentEntry = "{" + currentEntry;
											if (!currentEntry.endsWith("}"))
												currentEntry = currentEntry + "}";
											String nickname = extractValue(currentEntry, "nickname");
											String color = extractValue(currentEntry, "color");
											System.out.println("  처리 중인 참가자: Nick=" + nickname + ", Color=" + color);
											if (color != null && !color.isEmpty() && nickname != null) {
												String label = colorToNameMap.getOrDefault(color,
														nickname + " (" + color.substring(1) + ")");
												participantModel.addElement(label);
												if (!color.equals(myColor)) { // 자기 자신 제외
													System.out.println("    투표 목록에 추가: " + label);
													voteChoice.addItem(label);
													nameToColorMap.put(label, color);
												} else {
													System.out.println("    투표 목록에서 제외 (본인): " + label);
												}
											} else {
												System.out.println("    잘못된 참가자 데이터 건너뜀: " + currentEntry);
											}
										}
									} else {
										System.out.println("[클라이언트] 참가자 목록 데이터가 비어있습니다.");
									}
								} catch (Exception e) {
									System.err.println("[클라이언트] PARTICIPANTS 메시지 파싱 오류: " + finalLine);
									e.printStackTrace();
								}
							});
						}
						// 3. 질문 단계 (QUESTION_PHASE) 메시지 처리
						else if (finalLine.contains("\"type\":\"QUESTION_PHASE\"")) {
							// ... (기존 QUESTION_PHASE 처리 로직 동일) ...
							String question = extractValue(finalLine, "question");
							SwingUtilities.invokeLater(() -> {
								appendAnonymousChat("#444444", "❓ 질문: " + question);
								chatInput.setEnabled(true);
								chatInput.setBackground(Color.WHITE);
								chatInput.requestFocus();
								new Thread(() -> {
									/* ... 타이머 로직 ... */ }).start();
							});
						}
						// 4. 일반 채팅 (chat) 메시지 처리
						else if (finalLine.contains("\"type\":\"chat\"")) {
							// ... (기존 chat 처리 로직 동일) ...
							String color = extractValue(finalLine, "color");
							String msg = extractValue(finalLine, "message");
							SwingUtilities.invokeLater(() -> appendAnonymousChat(color, msg));
						}
						// 5. 답변 공개 (REVEAL_RESULT) 메시지 처리
						// 5. 답변 공개 (REVEAL_RESULT) 메시지 처리
						else if (finalLine.contains("\"type\":\"REVEAL_RESULT\"")) {
							// 답변 공개 안내 메시지 (EDT에서 실행)
							SwingUtilities.invokeLater(() -> appendAnonymousChat("#444444", "💬 모든 답변이 공개되었습니다!"));

							// 답변 목록 파싱 및 표시
							try {
								// answers 배열 부분 추출
								int answersStartIndex = finalLine.indexOf("\"answers\":[") + "\"answers\":[".length();
								int answersEndIndex = finalLine.lastIndexOf("]}"); // ]} 까지 포함

								if (answersEndIndex > answersStartIndex) {
									String answersData = finalLine.substring(answersStartIndex, answersEndIndex);
									// 개별 답변 객체 분리
									String[] items = answersData.split("\\}(?=\\s*,\\s*\\{)");

									System.out.println("[클라이언트] Parsing REVEAL_RESULT answers:"); // 파싱 시작 로그

									for (String item : items) {
										String currentItem = item.trim();
										// JSON 객체 형태 보정
										if (!currentItem.startsWith("{"))
											currentItem = "{" + currentItem;
										if (!currentItem.endsWith("}"))
											currentItem = currentItem + "}";

										String color = extractValue(currentItem, "color");
										String message = extractValue(currentItem, "message");

										// --- ❗ 각 답변 처리 로그 추가 ❗ ---
										System.out.println("  - Processing answer: Color=" + color + ", Msg="
												+ message.substring(0, Math.min(message.length(), 20)) + "...");

										// 유효한 데이터인지 간단히 확인
										if (color != null && !color.isEmpty() && message != null) {
											// 각 답변을 채팅창에 추가 (EDT에서 실행)
											final String finalColor = color; // 람다 사용 위해 final 변수로
											final String finalMessage = message;
											SwingUtilities.invokeLater(() -> {
												System.out
														.println("    -> appendAnonymousChat 호출: Color=" + finalColor); // append
																														// 호출
																														// 로그
												appendAnonymousChat(finalColor, "💬 " + finalMessage);
											});
										} else {
											System.out.println("    -> Invalid data, skipping append."); // 데이터 문제 로그
										}
										// --- 로그 추가 끝 ---
									}
								}
							} catch (Exception ex) {
								System.err.println("[클라이언트] REVEAL_RESULT 메시지 파싱 중 오류: " + finalLine);
								ex.printStackTrace();
							}
						}
						// 6. 투표 단계 (VOTE_PHASE) 메시지 처리
						else if (finalLine.contains("\"type\":\"VOTE_PHASE\"")) {
							// ... (기존 VOTE_PHASE 처리 로직 동일) ...
							SwingUtilities.invokeLater(() -> {
								appendAnonymousChat("#0000FF", "🗳️ 이제 투표할 시간입니다! ...");
								voteChoice.setEnabled(true);
								voteBtn.setEnabled(true);
								timerLabel.setVisible(false);
							});
						}
						// 7. 게임 시작 (GAME_START) 메시지 처리
						else if (finalLine.contains("\"type\":\"GAME_START\"")) {
							// ... (기존 GAME_START 처리 로직 동일 - 로그 추가된 버전) ...
							System.out.println("[클라이언트] GAME_START 메시지 감지됨!");
							SwingUtilities.invokeLater(() -> {
								try {
									System.out.println("[클라이언트 EDT] 게임 시작 UI 업데이트 시작...");
									gameStarted = true;
									startButton.setEnabled(false);
									startButton.setText("게임 진행 중...");
									System.out.println("[클라이언트 EDT] CardLayout 전환 시도...");
									cardLayout.show(chatContainerCards, "chat");
									System.out.println("[클라이언트 EDT] CardLayout 전환 완료.");
									appendAnonymousChat("#008000", "🎉 게임이 시작되었습니다! ...");
								} catch (Exception e) {
									/* ... 오류 처리 ... */ }
							});
						}
						// 8. 최종 결과 (FINAL_RESULT) 메시지 처리
						else if (finalLine.contains("\"type\":\"FINAL_RESULT\"")) {
							// 서버가 보낸 JSON에서 정보 추출
							String displayMsg = extractValue(finalLine, "message");
							List<String> winners = parseJsonList(finalLine, "winners"); // 승자 목록 파싱
							List<String> participants = parseJsonList(finalLine, "participants"); // 참가자 목록 파싱
							System.out.println("[클라이언트] Parsed winners: " + winners);
							System.out.println("[클라이언트] Parsed participants: " + participants);

							// 최종 결과 메시지 표시 및 UI 비활성화 (EDT)
							SwingUtilities.invokeLater(() -> {
								System.out.println("[클라이언트] 최종 결과 수신 처리 시작 (EDT)");
								appendAnonymousChat("#FF0000", displayMsg != null ? displayMsg : "게임 종료!");

								// UI 비활성화
								chatInput.setEnabled(false);
								chatInput.setBackground(Color.LIGHT_GRAY);
								voteChoice.setEnabled(false);
								voteBtn.setEnabled(false);
								startButton.setEnabled(false);
								startButton.setText("게임 종료");
								timerLabel.setText("게임 종료");
								timerLabel.setVisible(true);

								// --- ❗ 승/패 화면 전환 로직 ❗ ---
								System.out.println("[클라이언트] 결과 화면 전환 시도...");
								// 내 닉네임과 파싱된 리스트가 유효한지 확인
								if (permanentNickname != null && winners != null && participants != null) {
									try {
										// MafiaGResult 창 생성 및 표시 (내 닉네임, 승자 목록, 참가자 목록 전달)
										new MafiaGResult(permanentNickname, winners, participants);
										System.out.println("[클라이언트] MafiaGResult 창 생성 완료.");
										// 현재 PlayUI 창 닫기
										dispose(); // <<--- 현재 창 닫기
										System.out.println("[클라이언트] PlayUI 창 닫기 완료.");
									} catch (Exception e) {
										System.err.println("[클라이언트 오류] MafiaGResult 생성 또는 PlayUI 닫기 중 오류 발생");
										e.printStackTrace();
										JOptionPane.showMessageDialog(PlayUI.this, "결과 화면 전환 중 오류가 발생했습니다.", "오류",
												JOptionPane.ERROR_MESSAGE);
										closeConnection();
										dispose();
										System.exit(1);
									}
								} else {
									System.err.println("[클라이언트 오류] 결과 화면 전환에 필요한 정보 부족.");
									JOptionPane.showMessageDialog(PlayUI.this, "결과 처리 중 오류가 발생했습니다.", "오류",
											JOptionPane.ERROR_MESSAGE);
									closeConnection();
									dispose();
									System.exit(1);
								}
								// --- 화면 전환 로직 끝 ---
							});
						} // end of FINAL_RESULT handling

						// 9. 게임 오버 (GAME_OVER) 메시지 처리
						else if (finalLine.contains("\"type\":\"GAME_OVER\"")) {
							// ... (기존 GAME_OVER 처리 로직 동일) ...
							String msg = extractValue(finalLine, "message");
							SwingUtilities.invokeLater(() -> {
								appendAnonymousChat("#FF8C00", msg);
								/* ... UI 비활성화 ... */ timerLabel.setText("게임 종료됨");
								timerLabel.setVisible(true);
							});
						}

					} // end of while loop
				} catch (IOException e) {
					// ... (연결 끊김 처리 동일) ...
					if (sock != null && sock.isClosed()) {
						System.out.println("[클라이언트] 소켓 연결이 정상적으로 종료되었습니다.");
					} else {
						/* ... 연결 오류 처리 및 종료 ... */ }
				} finally {
					System.out.println("[클라이언트] 서버 리스너 스레드 종료.");
				}
			}); // end of thread definition
			serverThread.setDaemon(true);
			serverThread.start(); // 스레드 시작

		} catch (IOException e) {
			// ... (초기 연결 실패 처리 동일) ...
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "서버 연결 실패...", "오류", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	} // end of connectToServer

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

	private java.util.List<String> parseJsonList(String json, String key) {
		java.util.List<String> list = new java.util.ArrayList<>();
		try {
			String listKey = "\"" + key + "\":["; // 예: "\"winners\":["
			int startIndex = json.indexOf(listKey);
			if (startIndex == -1) {
				System.out.println("[parseJsonList] Key '" + key + "' not found in JSON.");
				return list;
			}

			startIndex += listKey.length(); // '[' 다음 위치
			int endIndex = json.indexOf("]", startIndex);
			if (endIndex == -1) {
				System.out.println("[parseJsonList] Closing ']' not found for key '" + key + "'.");
				return list;
			}

			String listContent = json.substring(startIndex, endIndex).trim();
			System.out.println("[parseJsonList] Content for key '" + key + "': [" + listContent + "]"); // 추출 내용 로그

			if (listContent.isEmpty())
				return list; // 빈 리스트면 종료

			// 콤마로 분리 (따옴표 안 콤마는 처리 못함 - 닉네임에 콤마 없다고 가정)
			String[] items = listContent.split(",");
			for (String item : items) {
				String trimmedItem = item.trim();
				// 앞뒤 따옴표 제거 및 unescape
				if (trimmedItem.startsWith("\"") && trimmedItem.endsWith("\"")) {
					String value = trimmedItem.substring(1, trimmedItem.length() - 1);
					value = value.replace("\\\"", "\"").replace("\\\\", "\\"); // 기본적인 unescape
					System.out.println("  -> Adding item: " + value); // 추가되는 항목 로그
					list.add(value);
				} else {
					System.out.println("  -> Invalid item format, skipping: " + trimmedItem); // 잘못된 형식 로그
				}
			}
		} catch (Exception e) {
			System.err.println("[클라이언트 오류] JSON 리스트 파싱 실패 - Key: " + key);
			e.printStackTrace();
		}
		return list;
	}

	private void closeConnection() {
		try {
			if (sock != null && !sock.isClosed()) {
				sock.shutdownInput(); // 👈 먼저 입력 스트림 닫기
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

	// JSON 문자열에 포함될 수 있는 특수문자를 escape 처리
	private String escapeJson(String str) {
		if (str == null)
			return null;
		// 기본적인 escape 처리 (더 필요한 경우 추가)
		return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t",
				"\\t");
	}

}
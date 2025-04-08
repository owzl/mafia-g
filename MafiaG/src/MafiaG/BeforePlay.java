package MafiaG;

	import java.awt.*;
	import java.awt.event.ActionEvent;
	import java.awt.event.ActionListener;
	import java.awt.event.WindowAdapter;
	import java.awt.event.WindowEvent;
	import java.awt.image.BufferedImage;
	import java.io.*;
	import java.net.InetAddress;
	import java.net.Socket;
	import java.net.UnknownHostException;
	import java.util.Scanner;

	import javax.swing.*;

	public class BeforePlay extends Frame implements ActionListener {
		private int readyCount = 0;
	    private final int MaxPlayers = 1;
		static Socket sock;
		static BufferedWriter bw;
		TextField tf;

		public BeforePlay() {
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					dispose();
				}
			});

			setTitle("MafiaG");
			setLayout(new BorderLayout());

			Panel bar = new Panel(); // 상단 로고, 닉네임
			Panel Chatroom = new Panel(); // 중앙 채팅방
			Panel West = new Panel(); // 랭킹, 참여자 명단, 시작 버튼칸

			bar.setLayout(new BorderLayout());

			// 상단 좌측 로고, 우측 랭킹, 닉네임 불러오기 칸
			ImageIcon logoimg = new ImageIcon("src/images/MafiaG_wordlogo.jpg");
			JLabel logo = new JLabel(logoimg);

			// database 연결 필요
			Panel rightPannel = new Panel(new FlowLayout(FlowLayout.RIGHT));
			String rank = "1";
			String name = "닉네임";
			JLabel ranking = new JLabel(rank + "위");
			JLabel nickname = new JLabel(name + "               "); // 너무 오른쪽에 붙어있는 점을 예방하기 위한 버퍼
			rightPannel.add(ranking);
			rightPannel.add(nickname);

			bar.add(logo, BorderLayout.WEST);
			bar.add(rightPannel, BorderLayout.EAST);

			add(bar, BorderLayout.NORTH);
			bar.setSize(1200, 50);

			// West 왼쪽편 기능들
			West.setLayout(new BorderLayout());
			 Label rankingLabel = new Label("랭킹", Label.CENTER);
		     rankingLabel.setBackground(new Color(230, 220, 250));
		     rankingLabel.setPreferredSize(new Dimension(150, 250));

		     TextArea participants = new TextArea("참여자 명단\nuser01\nuser02");
		     participants.setEditable(false);
		     participants.setPreferredSize(new Dimension(150, 100));

			Button start = new Button("Start"); // 3. start 버튼
			start.setPreferredSize(new Dimension(150, 50));
			start.setEnabled(true); // 임시로 활성화
			
//			if (totalPlayers > 1) { //2명 이상일때부터 시작 가능
//				start.setEnabled(true);
//			}
			
			start.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	                readyCount++;
	                start.setEnabled(false);
	                start.setLabel("대기 중...");
	                //다음창으로 넘어가기 위한 코드
	                if (readyCount >= MaxPlayers) { //수정 필요
	                    dispose(); //수정 필요
	                    new MafiaGGame("user01");
	                }
	            }
	        });

			West.add(rankingLabel, BorderLayout.NORTH);
			West.add(participants, BorderLayout.CENTER);
			West.add(start, BorderLayout.SOUTH);

			add(West, BorderLayout.WEST);

			// 채팅창 패널
			Chatroom.setLayout(new BorderLayout());

			// 튜토리얼 영역
			JLabel tutorial = new JLabel("튜토리얼 이미지 노출", JLabel.CENTER);
			tutorial.setFont(new Font("맑은 고딕", Font.BOLD, 28));
			tutorial.setOpaque(true);
			tutorial.setBackground(new Color(240, 240, 250));
			tutorial.setPreferredSize(new Dimension(600, 400));

			// 유저 입력창
			tf = new TextField();
			tf.setEditable(true);
			tf.addActionListener(this);
			tf.setBackground(new Color(230, 240, 250));
			tf.setPreferredSize(new Dimension(600, 50)); // 원하는 높이 설정

			// Chatroom에 위/아래로 추가
			Chatroom.add(tutorial, BorderLayout.CENTER);
			Chatroom.add(tf, BorderLayout.SOUTH);

			// Chatroom 자체를 CENTER에 배치
			add(Chatroom, BorderLayout.CENTER);
//			Chatroom.add("card2", talk);
			Chatroom.setSize(950, 500);


			setSize(1200, 800); // 중앙에서 노출되게 옮겨놔야..?
			setVisible(true);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String msg = e.getActionCommand();
			try {
				bw.write(msg);
				bw.newLine();
				bw.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			tf.setText("");
			
		}
}
//			InputStream is = null;
//			InputStreamReader isr = null;
//			BufferedReader br = null;
//
//			try {
//				InetAddress addr = null;
//				addr = InetAddress.getByAddress(new byte[] { (byte) 172, 30, 1, 71 }); // 서버 IP 변경 필요
//				sock = new Socket(addr, 3000);
//				OutputStream os = null;
//				OutputStreamWriter osw = null;
//
//				os = sock.getOutputStream();
//				osw = new OutputStreamWriter(os);
//				bw = new BufferedWriter(osw);
//
//			} catch (UnknownHostException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//
//		@Override
//		public void actionPerformed(ActionEvent e) {
//			String msg = e.getActionCommand();
//			try {
//				bw.write(msg);
//				bw.newLine();
//				bw.flush();
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}
//
//			tf.setText("");
//		}



//package MafiaG;
//
//import java.awt.*;
//import java.awt.event.*;
//import javax.swing.*;
//
//public class BeforePlay extends Frame {
//    private int readyCount = 0;
//    private final int totalPlayers = 1;
//
//    public BeforePlay() {
//        setTitle("MafiaG");
//        setLayout(new BorderLayout());
//
//        // 창 닫기 이벤트
//        addWindowListener(new WindowAdapter() {
//            public void windowClosing(WindowEvent e) {
//                dispose();
//            }
//        });
//
//        // 상단 바 - 로고 + 랭킹 닉네임
//        Panel topBar = new Panel(new BorderLayout());
//        ImageIcon logoIcon = new ImageIcon("src/images/MafiaG_wordlogo.jpg");
//        setIconImage(logoIcon.getImage());
//        JLabel logo = new JLabel(logoIcon);
//        topBar.add(logo, BorderLayout.WEST);
//
//        Panel rightInfo = new Panel(new FlowLayout(FlowLayout.RIGHT));
//        JLabel rank = new JLabel("랭킹");
//        JLabel nickname = new JLabel("닉네임");
//        rightInfo.add(rank);
//        rightInfo.add(nickname);
//        topBar.add(rightInfo, BorderLayout.EAST);
//
//        add(topBar, BorderLayout.NORTH);
//
//        // 왼쪽 - 랭킹 + 참여자 + 시작 버튼
//        Panel leftPanel = new Panel();
//        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
//
//        Label rankingLabel = new Label("랭킹", Label.CENTER);
//        rankingLabel.setBackground(new Color(230, 220, 250));
//        rankingLabel.setPreferredSize(new Dimension(150, 50));
//
//        TextArea participants = new TextArea("참여자 명단\nuser01\nuser02");
//        participants.setEditable(false);
//        participants.setPreferredSize(new Dimension(150, 300));
//
//        Button startBtn = new Button("시작");
//        startBtn.setPreferredSize(new Dimension(150, 50));
//        startBtn.setEnabled(true); // 임시로 활성화
//
//        startBtn.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                readyCount++;
//                startBtn.setEnabled(false);
//                startBtn.setLabel("대기 중...");
//
//                if (readyCount >= totalPlayers) {
//                    dispose();
//                    new MafiaGGame("user01");
//                }
//            }
//        });
//
//        leftPanel.add(rankingLabel);
//        leftPanel.add(participants);
//        leftPanel.add(startBtn);
//        add(leftPanel, BorderLayout.WEST);
//
//        // 중앙 - 튜토리얼 이미지 + 하단 텍스트
//        Panel centerPanel = new Panel(new BorderLayout());
//
//        ImageIcon tutorialImg = new ImageIcon("src/images/tutorial.jpg");
//        JLabel tutorial = new JLabel("튜토리얼 이미지 노출", JLabel.CENTER);
//        tutorial.setFont(new Font("맑은 고딕", Font.BOLD, 28));
//        tutorial.setOpaque(true);
//        tutorial.setBackground(new Color(240, 240, 250));
//        tutorial.setPreferredSize(new Dimension(600, 400));
//
//        Label waitLabel = new Label("참여자를 기다리는 중입니다...", Label.CENTER);
//        waitLabel.setBackground(new Color(230, 240, 250));
//
//        centerPanel.add(tutorial, BorderLayout.CENTER);
//        centerPanel.add(waitLabel, BorderLayout.SOUTH);
//        add(centerPanel, BorderLayout.CENTER);
//
//        setSize(1200, 800);
//        setLocationRelativeTo(null);
//        setVisible(true);
//    }
//}

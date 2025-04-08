package MafiaG;

import javax.swing.*;
import java.awt.*;

public class MafiaGGame extends JFrame {
    private String username;

    public MafiaGGame(String username) {
        this.username = username;

        setTitle("게임 플레이 화면");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JLabel infoLabel = new JLabel("여기는 게임 플레이 화면입니다.", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.BOLD, 24));
        add(infoLabel, BorderLayout.CENTER);

        // 버튼 패널
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 20));
        
        // 승리 버튼
        JButton winButton = new JButton("승리 결과 보기");
        winButton.addActionListener(e -> {
            dispose(); // 현재 창 닫고
            new MafiaGResult(username, true); // 승리 결과 창 띄우기
        });

        // 패배 버튼
        JButton loseButton = new JButton("패배 결과 보기");
        loseButton.addActionListener(e -> {
            dispose(); // 현재 창 닫고
            new MafiaGResult(username, false); // 패배 결과 창 띄우기
        });

        buttonPanel.add(winButton);
        buttonPanel.add(loseButton);

        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }
}

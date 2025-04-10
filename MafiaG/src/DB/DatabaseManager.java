package DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
	
	static {
	    try {
	        Class.forName("com.mysql.cj.jdbc.Driver");
	    } catch (ClassNotFoundException e) {
	        e.printStackTrace();
	    }
	}


	private static final String URL = "jdbc:mysql://172.30.1.47:3306/mafiag";
	private static final String USER = "dbeaver_user";
	public static String PASSWORD = "0000";

	// 로그인
	public static String checkLogin(String id, String password) {
		String sql = "SELECT * FROM member WHERE member_id = ? AND password = ?";
		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, id);
			pstmt.setString(2, password);

			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getString("nickname");
			} else {
				return null;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	// 아이디 찾기
	public static String findMemberIdByEmail(String email) {
		String sql = "SELECT member_id FROM member WHERE email = ?";
		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, email);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getString("member_id");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	// 비밀번호 찾기
	public static boolean findPasswordByEmailAndId(String id, String email) {
		String sql = "SELECT * FROM member WHERE member_id = ? AND email = ?";
		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, id);
			pstmt.setString(2, email);
			ResultSet rs = pstmt.executeQuery();
			return rs.next();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	// ID 중복 확인
	public static boolean isIdDuplicate(String id) {
		String sql = "SELECT COUNT(*) FROM member WHERE member_id = ?";
		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, id);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1) > 0;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	// 닉네임 중복 확인
	public static boolean isNicknameDuplicate(String nickname) {
		String sql = "SELECT COUNT(*) FROM member WHERE nickname = ?";
		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, nickname);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1) > 0;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	// 이메일 중복 확인
	public static boolean isEmailDuplicate(String email) {
		String sql = "SELECT COUNT(*) FROM member WHERE email = ?";
		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, email);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt(1) > 0;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	// 회원 가입 처리
	public static boolean insertNewMember(String id, String password, String nickname, String email) {
		String sql = "INSERT INTO member (member_id, password, email, nickname) VALUES (?, ?, ?, ?)";

		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, id);
			pstmt.setString(2, password);
			pstmt.setString(3, email);
			pstmt.setString(4, nickname);

			int rows = pstmt.executeUpdate();
			return rows > 0;

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	// 유저의 현재 점수 가져오기
	public static int getUserScore(String username) {
		int score = 0;
		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
			 PreparedStatement pstmt = conn.prepareStatement(
					 "SELECT score FROM member WHERE member_id = ?")) {

			pstmt.setString(1, username);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				score = rs.getInt("score");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return score;
	}

	// 게임 종료 후 점수 반영 (Gemini 제외, 동점자 모두 승리 처리)
	public static void updateScoresAfterGame(List<String> winners, List<String> participants) {
	    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
	        // Gemini가 득표수 1위일 경우
	        boolean geminiWins = winners.contains("Gemini");

	        // 참가자들 점수 업데이트
	        for (String player : participants) {
	            if (player.equals("Gemini")) continue;  // Gemini는 제외

	            int scoreChange = 0;

	            if (geminiWins) {
	                // Gemini가 승리한 경우: 모든 참가자에게 -2점 부여
	                scoreChange = -2;
	            } else {
	                // 참여자 승리 시, 동점자 처리
	                if (winners.contains(player)) {
	                    // 동점자 승리 시 +3점
	                    scoreChange = 3;
	                } else {
	                    // 패배자에게 -1점
	                    scoreChange = -1;
	                }
	            }

	            // 점수 업데이트 쿼리
	            try (PreparedStatement pstmt = conn.prepareStatement(
	                    "UPDATE member SET score = score + ? WHERE member_id = ?")) {
	                pstmt.setInt(1, scoreChange);
	                pstmt.setString(2, player);
	                pstmt.executeUpdate();
	            }
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}




	// 로그아웃 처리
	public static void logoutUser(String username) {
		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
			 PreparedStatement pstmt = conn.prepareStatement(
					 "UPDATE member SET last_login = NOW() WHERE member_id = ?")) {

			pstmt.setString(1, username);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// 랭킹 상위 유저 n명 가져오기
	public static List<UserScore> getTopRankers(int limit) {
		List<UserScore> rankers = new ArrayList<>();
		String sql = "SELECT nickname, score FROM member ORDER BY score DESC LIMIT ?";

		try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setInt(1, limit);
			ResultSet rs = pstmt.executeQuery();

			while (rs.next()) {
				String name = rs.getString("nickname");
				int score = rs.getInt("score");
				rankers.add(new UserScore(name, score));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return rankers;
	}
}
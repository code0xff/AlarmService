import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class AlarmService {
	private static Connection conn;
	private static Map<String, String> config;
	private static Map<String, String> sendUrlMap;
	private static Map<String, String> urlMap;
	private final static String USER_AGENT = "Mozilla/5.0";

	private AlarmService() {
		init();
		conn = getConnection();
	}

	public static void main(String[] args) {
		AlarmService aas = new AlarmService();
		while (true) {
			execute();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private static void execute() {
		List<AlarmInfo> sendList = getSendList();
		if (sendList.size() == 0) {
			System.out.println("등록되어 있는 알림 정보가 없습니다.");
			return;
		}
		sendEmail(sendList);
	}

	private static void init() {
		BufferedReader reader = null;
		StringTokenizer st = null;
		String line = "";
		String key = "";
		String val = "";
		
		config = new HashMap<String, String>();
		try {
			reader = new BufferedReader(new FileReader("askaboutbit.config"));
			while ((line = reader.readLine()) != null) {
				st = new StringTokenizer(line, "=");
				
				key = st.nextToken();
				val = st.nextToken();
				config.put(key, val);
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		sendUrlMap = new HashMap<String, String>();
		sendUrlMap.put("bithumb", "https://api.bithumb.com/public/ticker/$1");
		sendUrlMap.put("coinnest", "https://api.coinnest.co.kr/api/pub/ticker?coin=$1");
		sendUrlMap.put("coinrail", "https://api.coinrail.co.kr/public/last/order?currency=$1-krw");
		sendUrlMap.put("korbit", "https://api.korbit.co.kr/v1/ticker?currency_pair=$1_krw");
		sendUrlMap.put("upbit", "https://crix-api-endpoint.upbit.com/v1/crix/candles/days?code=CRIX.UPBIT.krw-$1");
		sendUrlMap.put("coinone", "https://api.coinone.co.kr/ticker/?currency=$1&format=json");

		urlMap = new HashMap<String, String>();
		urlMap.put("bithumb", "https://www.bithumb.com");
		urlMap.put("coinnest", "https://www.coinnest.co.kr");
		urlMap.put("coinrail", "https://www.coinrail.com");
		urlMap.put("korbit", "https://www.korbit.co.kr");
		urlMap.put("upbit", "https://upbit.com/coin_list");
		urlMap.put("coinone", "https://coinone.co.kr");
	}

	private static void sendEmail(List<AlarmInfo> sendList) {
		String host = "smtp.gmail.com";
		String username = config.get("username");
		String password = config.get("password");
		String recipient = "";
		String subject = "";
		String body = "";
		Session session = null;
		MimeMessage message = null;
		Properties props = null;
		Transport transport = null;
		int lastPrice = 0;
		int cnt = 0;

		for (AlarmInfo ai : sendList) {
			cnt++;
			lastPrice = sendGet(ai.getExchange(), ai.getCoin());

			if (ai.getPrice() > lastPrice)
				continue;

			recipient = ai.getEmail();
			subject = "거래소 " + ai.getExchange() + " " + ai.getCoin() + "의 알림요청하신 가격을 넘었습니다!";
			body = ai.getCreateDate() + "에 등록하신 거래소 " + ai.getExchange() + " " + ai.getCoin() + "의 가격은 현재 " + lastPrice
					+ "원으로 알림요청하신 가격 " + ai.getPrice() + "원을 넘었습니다.!";

			props = new Properties();
			props.put("mail.smtps.auth", "true");

			session = Session.getDefaultInstance(props);
			message = new MimeMessage(session);

			try {
				message.setSubject(subject);
				message.setText(body);
				message.setFrom(new InternetAddress(username));
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

				transport = session.getTransport("smtps");
				transport.connect(host, username, password);
				transport.sendMessage(message, message.getAllRecipients());
				transport.close();
			} catch (MessagingException e) {
				e.printStackTrace();
			}

			updateSendFlag(ai.getAlarmKey());
			if (cnt % 100 == 0) {
				try {
					conn.commit();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void updateSendFlag(String alarmKey) {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement("UPDATE alarm_info set send_flag = 'Y' where alarm_key = '" + alarmKey + "'");
			if (ps.executeUpdate() > 0) {
				System.out.println("알림정보 " + alarmKey + "의 send_flag 정보가 정상적으로 업데이트 되었습니다.");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (!ps.isClosed())
					ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static List<AlarmInfo> getSendList() {
		PreparedStatement ps = null;
		ResultSet rs = null;
		AlarmInfo ai = null;

		List<AlarmInfo> sendList = new ArrayList<AlarmInfo>();

		try {
			ps = conn.prepareStatement(
					"SELECT alarm_key, user_key, exchange, coin, price, email, create_date FROM alarm_info WHERE send_flag = 'N'");
			rs = ps.executeQuery();

			while (rs.next()) {
				ai = new AlarmInfo();

				ai.setAlarmKey(rs.getString("alarm_key"));
				ai.setUserKey(rs.getString("user_key"));
				ai.setExchange(rs.getString("exchange"));
				ai.setCoin(rs.getString("coin"));
				ai.setPrice(rs.getInt("price"));
				ai.setEmail(rs.getString("email"));
				ai.setCreateDate(rs.getDate("create_date"));

				System.out.println(ai.toString());
				sendList.add(ai);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (!rs.isClosed())
					rs.close();
				if (!ps.isClosed())
					ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return sendList;
	}

	private Connection getConnection() {
		Connection conn = null;

		try {
			Class.forName("org.postgresql.Driver");
			conn = DriverManager.getConnection(config.get("dbhost"), config.get("dbusername"),
					config.get("dbpassword"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		return conn;
	}

	private static int sendGet(String exchange, String coin) {
		String url = sendUrlMap.get(exchange).replace("$1", coin);
		int price = 0;

		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			con.setRequestMethod("GET");
			con.setRequestProperty("User-Agent", USER_AGENT);

			int responseCode = con.getResponseCode();
			System.out.println("\nSending 'GET' request to URL : " + url);
			System.out.println("Response Code : " + responseCode);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			System.out.println(response.toString());

			price = getPrice(exchange, response.toString());
			System.out.println("price: " + price);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return price;
	}

	private static int getPrice(String exchange, String json) throws Exception {
		int price = 0;
		double dTemp = 0d;
		long lTemp = 0l;
		JSONParser jp = new JSONParser();
		JSONObject jo = null;

		switch (exchange) {
		case "bithumb":
			jo = (JSONObject) jp.parse(json);
			jo = (JSONObject) jo.get("data");
			price = Integer.parseInt((String) jo.get("closing_price"));
			break;
		case "coinrail":
			jo = (JSONObject) jp.parse(json);
			price = Integer.parseInt((String) jo.get("last_price"));
			break;
		case "coinnest":
			jo = (JSONObject) jp.parse(json);
			lTemp = (long) jo.get("last");
			price = (int) lTemp;
			break;
		case "upbit":
			json = json.replace("[", "");
			json = json.replace("]", "");
			jo = (JSONObject) jp.parse(json);
			dTemp = (double) jo.get("tradePrice");
			price = (int) dTemp;
			break;
		default:
			jo = (JSONObject) jp.parse(json);
			price = Integer.parseInt((String) jo.get("last"));
			break;
		}
		return price;
	}
}

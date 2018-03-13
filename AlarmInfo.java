import java.util.Date;

public class AlarmInfo {
	String alarmKey;
	String userKey;
	String exchange;
	String coin;
	int price;
	String email;
	Date createDate;
	char sendFlag;
	Date sendDate;

	public String getAlarmKey() {
		return alarmKey;
	}

	public void setAlarmKey(String alarmKey) {
		this.alarmKey = alarmKey;
	}

	public String getUserKey() {
		return userKey;
	}

	public void setUserKey(String userKey) {
		this.userKey = userKey;
	}

	public String getExchange() {
		return exchange;
	}

	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public String getCoin() {
		return coin;
	}

	public void setCoin(String coin) {
		this.coin = coin;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public char getSendFlag() {
		return sendFlag;
	}

	public void setSendFlag(char sendFlag) {
		this.sendFlag = sendFlag;
	}

	public Date getSendDate() {
		return sendDate;
	}

	public void setSendDate(Date sendDate) {
		this.sendDate = sendDate;
	}

	@Override
	public String toString() {
		return "[" + alarmKey + ", " + userKey + ", " + exchange + ", " + coin + ", " + price + ", " + email + "]";
	}
}

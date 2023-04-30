package com.smart.service;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.springframework.stereotype.Service;

@Service
public class EmailService {
	public boolean sendMessage(String msg, String subject, String to) {

		boolean flag = false;

		String host = "smtp.gmail.com";

		Properties properties = System.getProperties();

		properties.put("mail.smtp.host", host);
		properties.put("mail.smtp.port", 465);
		properties.put("mail.smtp.ssl.enable", true);
		properties.put("mail.smtp.auth", true);

		Session session = Session.getInstance(properties, new Authenticator() {

			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				// TODO Auto-generated method stub
				return new PasswordAuthentication("komalkrishna016@gmail.com", "oigfzodbwzaogepi");
			}

		});

		session.setDebug(true);

		MimeMessage mimeMessage = new MimeMessage(session);

		try {

			mimeMessage.setFrom("komalkrishna016@gmail.com");

			mimeMessage.setSubject(subject);

			mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

//			mimeMessage.setText(msg);
			mimeMessage.setContent(msg, "text/html");

			Transport.send(mimeMessage);

			System.out.println("email sent successfully...");

			flag = true;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return flag;

	}
}

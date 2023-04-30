package com.smart.controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.service.EmailService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ForgotController {

	Random random = new Random(1000);

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private EmailService emailService;

	@Autowired
	private UserRepository userRepository;

	// handler to open email form
	@GetMapping("/forgot")
	public String openEmailForm() {
		return "forgot_email_form";
	}

	// handler to sendOtp
	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email, HttpSession session) {
		System.out.println("email : " + email);

		// generate 4 digit otp

		int otp = random.nextInt(100000);

		System.out.println("otp : " + otp);

		// code to send otp to email
		String msg = "<div style='border : 1px solid #e2e2e2; padding:20px'>" + "<h1>" + "OTP : " + "<b>" + otp + "</b>"
				+ "</h1>" + "</div>";
		String subject = "Otp Verfication - Smart Contact Manager";
		String to = email;

		boolean flag = this.emailService.sendMessage(msg, subject, to);

		if (flag) {
			session.setAttribute("myotp", otp);
			session.setAttribute("email", email);
			return "verify_otp";
		} else {
			session.setAttribute("msgg", "Check your email again..");
			return "forgot_email_form";
		}

	}

	// verify otp
	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam("otp") int otp, HttpSession session) {
		int my_otp = (int) session.getAttribute("myotp");
		String email = (String) session.getAttribute("email");

		if (my_otp == otp) {
			// otp is correct so change the password

			User user = this.userRepository.getUserByUsername(email);

			if (user == null) {
				// no user exist with the email
				session.setAttribute("msgg", "User doesn't exists with this email!!");
				return "forgot_email_form";
			} else {
				return "password_change_form";
			}

		} else {
			session.setAttribute("msgg", "you've entered wrong OTP!!");
			return "verify_otp";
		}
	}

	// change password
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newPassword") String newPassword, HttpSession session) {
		
		String email = (String) session.getAttribute("email");

		User user = this.userRepository.getUserByUsername(email);

		user.setPassword(this.bCryptPasswordEncoder.encode(newPassword));

		this.userRepository.save(user);
		
		return "redirect:/signin";
	}

}

package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.aspectj.weaver.NewConstructorTypeMunger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.smart.dao.ContactRepository;
import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.MyOrder;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private MyOrderRepository myOrderRepository;

	// now we used @ModelAttribute so every handler run this function to get user
	// details
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		// using this Principal class object we can get userName
		String userName = principal.getName();

		// now we can get User by using this userName
		User user = userRepository.getUserByUsername(userName);
//		System.out.println(user);
		model.addAttribute("user", user);
	}

	@GetMapping("/index")
	public String dashboard(Model model, Principal principal) {

		model.addAttribute("title", "Dashboard - Smart Contact Manager");

		// this "user_dashboard.html" is inside normal folder so we specified like this
		return "normal/user_dashboard";
	}

	// handler to add contact
	@GetMapping("/add-contact")
	public String addContact(Model model) {
		model.addAttribute("title", "Add Contact - Smart Contact Manager");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact";
	}

	// handle to process add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute("contact") Contact contact,
			@RequestParam("profileImage") MultipartFile file, Principal principal, HttpSession session) {

		try {
			String name = principal.getName();

			User user = this.userRepository.getUserByUsername(name);

			if (file.isEmpty()) {
				System.out.println("File is empty...");
				contact.setImage("default.png");
			} else {
				// saving the original file name in contact database
				contact.setImage(file.getOriginalFilename());

				// now get the path where we want to store this image
				// so want to store in "static/img"

				File saveFile = new ClassPathResource("static/img/").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				System.out.println("Image is uploaded...");

			}

			// Many to one mapping so set the contact to the user
			// then from user get the contacts and add the contact
			contact.setUser(user);

			user.getContacts().add(contact);

			this.userRepository.save(user);

			// success alert
			session.setAttribute("msg", new Message("Contact added successfully...", "alert-success"));

			System.out.println("data added in database");
		} catch (Exception e) {
			System.out.println("Error : " + e.getMessage());

			// error alert
			session.setAttribute("msg", new Message("Something went wrong!! Please try again...", "alert-danger"));

			e.printStackTrace();
		}
//		System.out.println("Details : " + contact);
		return "normal/add_contact";
	}

	// handler for show contacts
	// per page show 5 contacts
	// current page = 0 (denoted page variable)
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {
		model.addAttribute("title", "Show Contacts - Smart Contact Manager");

		String name = principal.getName();

		User user = this.userRepository.getUserByUsername(name);

		int uid = user.getId();

		Pageable pageable = PageRequest.of(page, 5);
		Page<Contact> contacts = this.contactRepository.findContactsByUser(uid, pageable);

		System.out.println(contacts);

		model.addAttribute("contacts", contacts);
		model.addAttribute("currPage", page);
		model.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show_contacts";
	}

	// showing particular contact details
	@GetMapping("/contact/{cId}")
	public String showContactDetails(@PathVariable("cId") Integer cid, Model model, Principal principal) {

		String name = principal.getName();

		User user = this.userRepository.getUserByUsername(name);

		Optional<Contact> contactDetails = this.contactRepository.findById(cid);
		Contact contact = contactDetails.get();

		if (user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}

		return "normal/contact_detail";
	}

	// delete contact
	@GetMapping("/delete/{cId}")
	public String deleteContact(@PathVariable("cId") Integer cid, HttpSession session, Principal principal) {

		Optional<Contact> optional = this.contactRepository.findById(cid);
		Contact contact = optional.get();

		User user = this.userRepository.getUserByUsername(principal.getName());
		user.getContacts().remove(contact);

		this.userRepository.save(user);

		this.contactRepository.delete(contact);
		session.setAttribute("msg", new Message("Contact deleted successfully...", "alert-success"));

		System.out.println("deletion done " + cid);

		return "redirect:/user/show-contacts/0";
	}

	// handler to update contact
	@GetMapping("/update/{cId}")
	public String updateContact(@PathVariable("cId") Integer cid, Model m) {

		Contact contact = this.contactRepository.findById(cid).get();

		m.addAttribute("contact", contact);
		m.addAttribute("title", contact.getName());
		return "normal/update_detail";
	}

	// handler to update process of update contact
	@PostMapping("/process-update")
	public String proceeUpdate(@ModelAttribute("contact") Contact contact,
			@RequestParam("profileImage") MultipartFile file, HttpSession session, Principal principal) {

		Contact oldContact = this.contactRepository.findById(contact.getcId()).get();

		try {

			if (!file.isEmpty()) {
				// delete old photo

				File deleteFile = new ClassPathResource("static/img/").getFile();
				File file1 = new File(deleteFile, oldContact.getImage());
				file1.delete();

				// update new photo
				File saveFile = new ClassPathResource("static/img/").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				contact.setImage(file.getOriginalFilename());

			} else {
				contact.setImage(oldContact.getImage());
			}

			User user = this.userRepository.getUserByUsername(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);

			session.setAttribute("msg", new Message("Contact updated successfully...", "alert-success"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		return "redirect:/user/update/" + contact.getcId();
	}

	// handler for profile
	@GetMapping("/profile")
	public String profile() {
		return "normal/profile";
	}

	// handler to open settings
	@GetMapping("/settings")
	public String openSettings() {
		return "normal/settings";
	}

	// handler to change password
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPass,
			@RequestParam("newPassword") String newPass, Principal principal, HttpSession session) {

		System.out.println("old : " + oldPass);
		System.out.println("new : " + newPass);

		// firstly we match the old password is correct or not, our password is
		// encrypted so we use BcrptPasswordEncoder
		String name = principal.getName();

		User user = this.userRepository.getUserByUsername(name);

		if (this.bCryptPasswordEncoder.matches(oldPass, user.getPassword())) {
			// password matches so change the password
			user.setPassword(this.bCryptPasswordEncoder.encode(newPass));
			this.userRepository.save(user);

			session.setAttribute("msg", new Message("Password changed successfully...", "alert-success"));
		} else {
			session.setAttribute("msg", new Message("Wrong old password...", "alert-danger"));
			return "redirect:/user/settings";
		}
		return "redirect:/user/index";
	}

	// handler for creating order for payment
	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String, Object> data, Principal principal) throws RazorpayException {
//		System.out.println("order function executed...");
//		System.out.println(data);

		int amt = Integer.parseInt(data.get("amount").toString());

		RazorpayClient razorpayClient = new RazorpayClient("rzp_test_sLbdmyMnjLXT8K", "2eLL2tLXW8QiUyv21oqReH6h");

		JSONObject ob = new JSONObject();

		ob.put("amount", amt * 100); // amount is in paise so multipling with 100
		ob.put("currency", "INR");
		ob.put("receipt", "txn_123456");
		
		Order order = razorpayClient.orders.create(ob);
		
		System.out.println(order);
		
		// save order in database
		MyOrder myOrder = new MyOrder();
		myOrder.setAmount(order.get("amount")+"");
		myOrder.setOrderId(order.get("id"));
		myOrder.setReceipt(order.get("receipt"));
		myOrder.setPaymentId(null);
		myOrder.setStatus(order.get("status"));
		myOrder.setUser(this.userRepository.getUserByUsername(principal.getName()));
		
		this.myOrderRepository.save(myOrder);
		
		
		// we can save this order in our data

		return order.toString();
	}
	
	@PostMapping("/update_order")
	@ResponseBody
	public ResponseEntity<?> updateOrder(@RequestBody Map<String, Object> data){
		System.out.println(data);
		
		MyOrder myOrder = this.myOrderRepository.findByOrderId(data.get("order_id").toString());
		
		myOrder.setPaymentId(data.get("payment_id").toString());
		myOrder.setStatus(data.get("status").toString());
		
		this.myOrderRepository.save(myOrder);
		
		
		return ResponseEntity.ok(Map.of("msg", "updated"));
	}

}

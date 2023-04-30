console.log("Script is running...");

const toggleSidebar = () => {
	if ($(".sidebar").is(":visible")) {
		// sidebar is visible so hide sidebar and make content margin-left 0%
		$(".sidebar").css("display", "none");
		$(".content").css("margin-left", "0%");
	}
	else {
		$(".sidebar").css("display", "block");
		$(".content").css("margin-left", "20%");
	}
}

const search = () => {
	/*console.log("searching....");*/

	let query = $("#search-input").val();

	if (query == "") {
		$(".search-result").hide();
	}
	else {
		console.log(query);

		// sending request to server
		let url = `http://localhost:8080/search/${query}`;

		fetch(url)
			.then((response) => {
				return response.json();
			}).then((data) => {
				// data.....
				console.log(data);

				let text = `<div class='list-group'>`;

				data.forEach((contact) => {
					text += `<a href='/user/contact/${contact.cId}' class='list-group-item list-group-item-action'>${contact.name}</a>`;
				})

				text += `</div>`;

				$(".search-result").html(text);
				$(".search-result").show();

			});




		$(".search-result").show();
	}




}

// first request to server to create order
const paymentStart = () => {
	console.log("payment started...");
	let amount = $("#payment_field").val();

	if (amount == '' || amount == null) {
		alert("Amount is required!!");
		return;
	}

	console.log(amount);

	// code
	// using ajax, send request to server to create order

	$.ajax({
		url: '/user/create_order',
		data: JSON.stringify({ amount: amount, info: 'order_request' }),
		contentType: 'application/json',
		type: 'POST',
		dataType: 'json',
		success: function(response) {
			// invoke when success
			console.log(response);

			if (response.status == "created") {
				// open payment form

				let options = {
					key: "rzp_test_sLbdmyMnjLXT8K",
					amount: response.amount,
					currency: "INR",
					name: "Smart Contact Manager",
					description: "Test txn",
					image: "https://i.pinimg.com/564x/2d/12/7b/2d127b10bfd155931c78ebd23e606d77.jpg",
					order_id: response.id,
					handler: function(res) {
						console.log(res.razorpay_order_id);
						console.log(res.razorpay_payment_id);
						console.log(res.razorpay_signature);
						console.log("payment successful!!");

						updatePaymentOnServer(res.razorpay_order_id, res.razorpay_payment_id, "paid");

						
					},
					prefill: {
						name: "",
						email: "",
						contact: ""
					},
					notes: {
						address: "KM Residency"
					},
					theme: {
						color: "#3399cc"
					}
				};

				let rzp = new Razorpay(options);

				rzp.on('payment.failed', function(response) {
					console.log(response.error.code);
					console.log(response.error.description);
					console.log(response.error.source);
					console.log(response.error.step);
					console.log(response.error.reason);
					console.log(response.error.metadata.order_id);
					console.log(response.error.metadata.payment_id);
					alert("Oops!! payment failed...");
				});

				rzp.open();

			}
		},
		error: function(error) {
			// invoke when error
			console.log(error);
			alert("Something went wrong!!");
		}
	})

};

function updatePaymentOnServer(order_id, payment_id, status)
{
	$.ajax({
		url: '/user/update_order',
		data: JSON.stringify({ order_id: order_id, payment_id: payment_id, status: status }),
		contentType: 'application/json',
		type: 'POST',
		dataType: 'json',
		success:function(response){
			alert("payment successful!!");
		},
		error:function(error){
			alert("payment successful! but not captured on server..");
		}
	});
}











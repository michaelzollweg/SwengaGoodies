package at.fh.swenga.jpa.controller;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ExceptionHandler;

import at.fh.swenga.jpa.dao.EmployeeRepository;
import at.fh.swenga.jpa.model.EmployeeModel;

@Controller
public class ReportController {
	@Autowired
	EmployeeRepository employeeRepository;

	@Autowired
	private MailSender mailSender;
	@Autowired
	private SimpleMailMessage templateMessage;

	@RequestMapping(value = { "/report" })
	public String report(Model model, @RequestParam(required = false) String excel,
			@RequestParam(required = false) String pdf, @RequestParam(required = false) String mail,
			@RequestParam(name = "employeeId", required = false) List<Integer> employeeIds) {

		// User didn't select any employee ? -> go back to list
		if (CollectionUtils.isEmpty(employeeIds)) {
			model.addAttribute("errorMessage", "No employees selected!");
			return "forward:/list";
		}

		// Convert the list of ids to a list of employees.
		// The method findAll() can do this
		List<EmployeeModel> employees = employeeRepository.findAllById(employeeIds);

		// Store the employees in the model, so the reports can access them
		model.addAttribute("employees", employees);

		// Which submit button was pressed? -> call the right report view
		if (StringUtils.isNoneEmpty(excel)) {
			return "excelReport";
		} else if (StringUtils.isNoneEmpty(pdf)) {
			return "pdfReport";
			// return "pdfReportV5";

		} else if (StringUtils.isNoneEmpty(mail)) {
			sendMail(employees);
			model.addAttribute("errorMessage", "Mail sent");
			return "forward:/list";
		}

		else {
			return "forward:/list";
		}
	}

	private void sendMail(List<EmployeeModel> employees) {

		String content = "";
		for (EmployeeModel employee : employees) {
			content += employee.getFirstName() + " " + employee.getLastName() + "\n";
		}

		// Create a thread safe "copy" of the template message and customize it
		SimpleMailMessage msg = new SimpleMailMessage(this.templateMessage);

		// You can override default settings from dispatcher-servlet.xml:
		// msg.setFrom(from);
		// msg.setTo(to);
		// msg.setSubject(subject);
		msg.setText(String.format(msg.getText(), "Max Mustermann", content));
		try {
			this.mailSender.send(msg);
		} catch (MailException ex) {
			ex.printStackTrace();
		}
	}

	@ExceptionHandler(Exception.class)
	public String handleAllException(Exception ex) {
		return "error";
	}

}

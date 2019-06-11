package at.fh.swenga.jpa.controller;

import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.fluttercode.datafactory.impl.DataFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.ExceptionHandler;

import at.fh.swenga.jpa.dao.DocumentRepository;
import at.fh.swenga.jpa.dao.EmployeeRepository;
import at.fh.swenga.jpa.model.DocumentModel;
import at.fh.swenga.jpa.model.EmployeeModel;

@Controller
public class EmployeeController {

	@Autowired
	EmployeeRepository employeeRepository;

	@Autowired
	DocumentRepository documentRepository;

	@RequestMapping(value = { "/", "list" })
	public String index(Model model) {
		List<EmployeeModel> employees = employeeRepository.findAll();
		model.addAttribute("employees", employees);
		return "index";
	}

	@RequestMapping("/fill")
	@Transactional
	public String fillData(Model model) {

		DataFactory df = new DataFactory();

		for (int i = 0; i < 10; i++) {
			EmployeeModel p1 = new EmployeeModel(df.getFirstName(), df.getLastName());
			employeeRepository.save(p1);
		}
		return "forward:list";
	}

	@RequestMapping("/delete")
	public String deleteData(Model model, @RequestParam int id) {
		employeeRepository.deleteById(id);

		return "forward:list";
	}
	
	/**
	 * Display the upload form
	 * @param model
	 * @param employeeId
	 * @return
	 */
	@RequestMapping(value = "/upload", method = RequestMethod.GET)
	public String showUploadForm(Model model, @RequestParam("id") int employeeId) {
		model.addAttribute("employeeId", employeeId);
		return "uploadFile";
	}
	
	
	/**
	 * Save uploaded file to the database (as 1:1 relationship to employee)
	 * 
	 * @param model
	 * @param employeeId
	 * @param file
	 * @return
	 */
	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public String uploadDocument(Model model, @RequestParam("id") int employeeId,
			@RequestParam("myFile") MultipartFile file) {
 
		try {
 
			Optional<EmployeeModel> employeeOpt = employeeRepository.findById(employeeId);
			if (!employeeOpt.isPresent()) throw new IllegalArgumentException("No employee with id "+employeeId);
 
			EmployeeModel employee = employeeOpt.get();
 
			// Already a document available -> delete it
			if (employee.getDocument() != null) {
				documentRepository.delete(employee.getDocument());
				// Don't forget to remove the relationship too
				employee.setDocument(null);
			}
 
			// Create a new document and set all available infos
 
			DocumentModel document = new DocumentModel();
			document.setContent(file.getBytes());
			document.setContentType(file.getContentType());
			document.setCreated(new Date());
			document.setFilename(file.getOriginalFilename());
			document.setName(file.getName());
			employee.setDocument(document);
			employeeRepository.save(employee);
		} catch (Exception e) {
			model.addAttribute("errorMessage", "Error:" + e.getMessage());
		}
 
		return "forward:/list";
	}
	
	@RequestMapping("/download")
	public void download(@RequestParam("documentId") int documentId, HttpServletResponse response) {
 
		Optional<DocumentModel> docOpt = documentRepository.findById(documentId);
		if (!docOpt.isPresent()) throw new IllegalArgumentException("No document with id "+documentId);
 
		DocumentModel doc = docOpt.get();
 
		try {
			response.setHeader("Content-Disposition", "inline;filename=\"" + doc.getFilename() + "\"");
			OutputStream out = response.getOutputStream();
				// application/octet-stream
			response.setContentType(doc.getContentType());
			out.write(doc.getContent());
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
 

	@ExceptionHandler(Exception.class)
	public String handleAllException(Exception ex) {
		return "error";
	}
	
}

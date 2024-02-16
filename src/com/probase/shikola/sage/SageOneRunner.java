package com.probase.shikola.sage;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.rmi.registry.Registry;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.prefs.Preferences;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.sql.PreparedStatement;

public class SageOneRunner {

	private static Preferences prefs;
	private final static String USER_AGENT = "Mozilla/5.0";
	
	public static void main(String[] args) throws Exception {

		prefs = Preferences.userRoot().node("Shikola");
		String username = prefs.get("username", null);
		String password = prefs.get("password", null);
		String sageUsername = prefs.get("sageUsername", null);
		String sagePassword = prefs.get("sagePassword", null);
		String confirmationCode = prefs.get("confirmationCode", null);
		String schoolName = prefs.get("schoolName", null);
		String preferredUrl = prefs.get("preferredUrl", null);
		String default_currency = prefs.get("default_currency", null);
		String term_spec = prefs.get("term_spec", null);
		String tuition_level = prefs.get("tuition_level", null);
		String fault_lang = prefs.get("default_lang", null);
		String fee_payment_type = prefs.get("fee_payment_type", null);
		Boolean fee_breakdown_available = prefs.getBoolean("fee_breakdown_available", false);
		String job_cycle = prefs.get("job_cycles", null);
		
		
		String myDriver = "org.gjt.mm.mysql.Driver";
	    String myUrl = "jdbc:mysql://localhost/shikola_sage";
	    Class.forName(myDriver);
	    Connection conn = DriverManager.getConnection(myUrl, "root", "");
	    
	    String authorizationKey =  sageUsername + ":" + sagePassword;
	    String authorizationKeyEncoded = Base64.encodeBase64String(authorizationKey.getBytes());
	    System.out.println("authorizationKeyEncoded ..." + authorizationKeyEncoded);
	    //pullStudents(conn, authorizationKeyEncoded, username, password, confirmationCode, default_currency);
	    //pullPaymentComponents(conn, authorizationKeyEncoded, username, password, confirmationCode);
	    //setupSageIntegration();
	    //test();
	    pullCurrentStudentSchoolFeeQuery(conn, authorizationKeyEncoded, username, password, confirmationCode, default_currency);
	}
	
	public static void test() throws Exception
	{
		URL wsdlURL = new URL("http://localhost:5000/freedom.core/SageOneTest/SDK?wsdl");
		QName SERVICE_NAME = new QName("http://apache.org/hello_world_soap_http", "SOAPService");
	}
	
	public static void pullStudents(Connection conn, String authorizationKeyEncoded, String username, String password, String confirmationCode, 
			String default_currency)
	{
		try
		{
			SageApi sageApi = new SageApi();
			
			
			String query = "Select * from students_pulled order by id DESC limit 0, 1";
			Statement statement = conn.createStatement();
			ResultSet rs = statement.executeQuery(query);
			
			String lastStudentDate = null;
			while(rs.next())
			{
				lastStudentDate = rs.getString("student_created_date_online");
			}
			System.out.println("lastStudentDate..." + lastStudentDate);
			
			String baseUrl = "http://shikola.com/api/api-authenticate/" + URLEncoder.encode(username, "UTF-8") + "/" + URLEncoder.encode(password, "UTF-8")+"/";
			System.out.println("baseUrl..." + baseUrl);
			JSONObject jsObj = new JSONObject();
			String response = UtilityHelper.sendGet(baseUrl, null, jsObj);
			System.out.println(response);
			if(response!=null)
			{
				JSONObject responseJSONObject = new JSONObject(response);
				if(responseJSONObject!=null && responseJSONObject.has("token"))
				{
					String token = responseJSONObject.getString("token");
					System.out.println(token);
					if(lastStudentDate != null)
					{
						baseUrl = "http://shikola.com/api/school-students-query-by-last-created-date/" + URLEncoder.encode(confirmationCode, "UTF-8") + "/" + lastStudentDate;
					}
					else
					{
						baseUrl = "http://shikola.com/api/school-students-query-by-last-created-date/" + URLEncoder.encode(confirmationCode, "UTF-8");
					}
					System.out.println(baseUrl);
					jsObj = new JSONObject();
					response = UtilityHelper.sendGet(baseUrl, "token=" + token, jsObj);
					System.out.println(response);
					if(response!=null)
					{
						responseJSONObject = new JSONObject(response);
						Boolean status = responseJSONObject.getBoolean("status");
						if(status.equals(Boolean.TRUE))
						{
							String students = responseJSONObject.getString("students");
							JSONArray respArray = new JSONArray(students);
							for(int i=0; i<respArray.length(); i++)
							{
								JSONObject respArrayEntry = respArray.getJSONObject(i);
								System.out.println("respArrayEntry.." + respArrayEntry.toString());
								String student_reg_no = respArrayEntry.getString("student_reg_no");// + ((new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date()));
								String customerFound = sageApi.findCustomer(authorizationKeyEncoded, student_reg_no);
								if(customerFound==null)
								{
									String student_id = respArrayEntry.getString("student_id");
									String first_name = respArrayEntry.getString("first_name").toUpperCase();
									String last_name = respArrayEntry.getString("last_name").toUpperCase();
									String other_name = respArrayEntry.has("other_name") && !respArrayEntry.isNull("other_name") ? respArrayEntry.getString("other_name").toUpperCase() : null;
									Double account_balance = 0.00;
									Boolean charge_tax = false;
									String currency = default_currency;
									String mobile_number = respArrayEntry.has("mobile") && !respArrayEntry.isNull("mobile") ? respArrayEntry.getString("mobile") : null;
									String email_address = respArrayEntry.has("email") && !respArrayEntry.isNull("email") ? respArrayEntry.getString("email") : null;
									String gender = respArrayEntry.has("gender") && !respArrayEntry.isNull("gender") ? respArrayEntry.getString("gender") : null;
									String created_at = respArrayEntry.getString("created_at");
									Date created_at_date = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse(created_at);
									java.sql.Date dateCreated = new java.sql.Date(created_at_date.getTime());
									if(gender!=null)
									{
										gender = gender.toUpperCase();
									}
									else
									{
										gender = "MALE";
									}
									String newCustomer = sageApi.createNewCustomer(authorizationKeyEncoded, student_reg_no, first_name, last_name,
											other_name, account_balance, charge_tax, currency, 
											mobile_number, email_address, gender);
									query = " insert into students_pulled (student_id_online, student_created_date_online, created_at, updated_at)"
											+ " values (?, ?, ?, ?)";
		
									// create the mysql insert preparedstatement
									PreparedStatement preparedStmt = conn.prepareStatement(query);
									preparedStmt.setString (1, student_id);
									preparedStmt.setDate   (2, dateCreated);
									preparedStmt.setDate   (3, dateCreated);
									preparedStmt.setDate   (4, dateCreated);
									//execute the preparedstatement
									preparedStmt.execute();
									System.out.println("Name..." + first_name + " " + last_name + "("+student_reg_no+")");
								}
							}
						}
					}
					
					
					
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	public static void pullPaymentComponents(Connection conn, String authorizationKeyEncoded, 
			String username, String password, String confirmationCode)
	{
		try
		{
			SageApi sageApi = new SageApi();
			String baseUrl = "http://shikola.com/api/api-authenticate/" + URLEncoder.encode(username, "UTF-8") + "/" + URLEncoder.encode(password, "UTF-8")+"/";
			System.out.println("baseUrl..." + baseUrl);
			JSONObject jsObj = new JSONObject();
			String response = UtilityHelper.sendGet(baseUrl, null, jsObj);
			System.out.println(response);
			if(response!=null)
			{
				JSONObject responseJSONObject = new JSONObject(response);
				if(responseJSONObject!=null && responseJSONObject.has("token"))
				{
					String token = responseJSONObject.getString("token");
					System.out.println(token);
					baseUrl = "http://shikola.com/api/get-all-payment-items/" + URLEncoder.encode(confirmationCode, "UTF-8");
					System.out.println(baseUrl);
					jsObj = new JSONObject();
					response = UtilityHelper.sendGet(baseUrl, "token=" + token, jsObj);
					System.out.println(response);
					if(response!=null)
					{
						responseJSONObject = new JSONObject(response);
						Boolean status = responseJSONObject.getBoolean("status");
						if(status.equals(Boolean.TRUE))
						{
							JSONArray respArray = responseJSONObject.getJSONArray("paymentItems");
							
							for(int i=0; i<respArray.length(); i++)
							{
								JSONObject respArrayEntry = respArray.getJSONObject(0);
								System.out.println("respArrayEntry.." + respArrayEntry.toString());
								String itemCode = respArrayEntry.getString("id");// + ((new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date()));
								String itemFound = sageApi.findItem(authorizationKeyEncoded, itemCode);
								System.out.println("itemFound..." + itemFound);
								if(itemFound==null)
								{
									Boolean isActive = true;
									Boolean isServiceItem = true;
									Boolean isWarehouseTracked = false;
									String itemDescription1 = respArrayEntry.getString("name");
									String itemDescription2 = respArrayEntry.getString("desc").toUpperCase();
									String itemDescription3 = "Is Tuition: " + respArrayEntry.getString("isTuition").toUpperCase();
									String newItem = sageApi.createNewItem(authorizationKeyEncoded, itemCode, 
											itemDescription1, itemDescription2, itemDescription3, 
											isActive, isWarehouseTracked, isServiceItem);
									System.out.println("Name..." + itemDescription1 + " " + itemDescription2 + "("+itemDescription3+")");
								}
							}
						}
					}
					
					
					
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	
	public static void pullCurrentStudentSchoolFeeQuery(Connection conn, String authorizationKeyEncoded, 
			String username, String password, String confirmationCode, String default_currency)
	{
		try
		{
			SageApi sageApi = new SageApi();
			String baseUrl = "http://shikola.com/api/api-authenticate/" + URLEncoder.encode(username, "UTF-8") + "/" + URLEncoder.encode(password, "UTF-8")+"/";
			System.out.println("baseUrl..." + baseUrl);
			JSONObject jsObj = new JSONObject();
			String response = UtilityHelper.sendGet(baseUrl, null, jsObj);
			System.out.println(response);
			if(response!=null)
			{
				JSONObject responseJSONObject = new JSONObject(response);
				if(responseJSONObject!=null && responseJSONObject.has("token"))
				{
					String token = responseJSONObject.getString("token");
					System.out.println(token);
					baseUrl = "http://shikola.com/api/school-students-query/" + confirmationCode;
					String parameters = "token=" + token;
					System.out.println(baseUrl);
					jsObj = new JSONObject();
					response = UtilityHelper.sendGet(baseUrl, parameters, jsObj);
					System.out.println(response);
					if(response!=null)
					{
						responseJSONObject = new JSONObject(response);
						Boolean status = responseJSONObject.getBoolean("status");
						if(status.equals(Boolean.TRUE))
						{
							String studentsList = responseJSONObject.getString("students");
							JSONArray studentsArray = new JSONArray(studentsList);
							
							//for(int i=0; i<studentsArray.length(); i++)
							//{
								JSONObject studentsArrayEntry = studentsArray.getJSONObject(0);
								String student_reg_no = studentsArrayEntry.getString("student_reg_no");// + ((new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date()));
								String customerFound = sageApi.findCustomer(authorizationKeyEncoded, student_reg_no);
								if(customerFound==null)
								{
									String student_id = studentsArrayEntry.getString("student_id");
									String first_name = studentsArrayEntry.getString("first_name").toUpperCase();
									String last_name = studentsArrayEntry.getString("last_name").toUpperCase();
									String other_name = studentsArrayEntry.has("other_name") && !studentsArrayEntry.isNull("other_name") ? studentsArrayEntry.getString("other_name").toUpperCase() : null;
									Double account_balance = 0.00;
									Boolean charge_tax = false;
									String currency = default_currency;
									String mobile_number = studentsArrayEntry.has("mobile") && !studentsArrayEntry.isNull("mobile") ? studentsArrayEntry.getString("mobile") : null;
									String email_address = studentsArrayEntry.has("email") && !studentsArrayEntry.isNull("email") ? studentsArrayEntry.getString("email") : null;
									String gender = studentsArrayEntry.has("gender") && !studentsArrayEntry.isNull("gender") ? studentsArrayEntry.getString("gender") : null;
									String created_at = studentsArrayEntry.getString("created_at");
									Date created_at_date = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse(created_at);
									java.sql.Date dateCreated = new java.sql.Date(created_at_date.getTime());
									if(gender!=null)
									{
										gender = gender.toUpperCase();
									}
									else
									{
										gender = "MALE";
									}
									String newCustomer = sageApi.createNewCustomer(authorizationKeyEncoded, student_reg_no, first_name, last_name,
											other_name, account_balance, charge_tax, currency, 
											mobile_number, email_address, gender);
									String query = " insert into students_pulled (student_id_online, student_created_date_online, created_at, updated_at)"
											+ " values (?, ?, ?, ?)";
		
									// create the mysql insert preparedstatement
									PreparedStatement preparedStmt = conn.prepareStatement(query);
									preparedStmt.setString (1, student_id);
									preparedStmt.setDate   (2, dateCreated);
									preparedStmt.setDate   (3, dateCreated);
									preparedStmt.setDate   (4, dateCreated);
									//execute the preparedstatement
									preparedStmt.execute();
									System.out.println("Name..." + first_name + " " + last_name + "("+student_reg_no+")");
									
									customerFound = sageApi.findCustomer(authorizationKeyEncoded, student_reg_no);
								}
								
								
								
								
								baseUrl = "http://shikola.com/api/student-current-school-fee-query";
								parameters = "token=" + token + "&student_reg_no="+ student_reg_no +"&confirmation_code=" + confirmationCode;
								System.out.println(baseUrl);
								jsObj = new JSONObject();
								response = UtilityHelper.sendGet(baseUrl, parameters, jsObj);
								System.out.println(response);
								if(response!=null)
								{
									responseJSONObject = new JSONObject(response);
									status = responseJSONObject.getBoolean("status");
									if(status.equals(Boolean.TRUE))
									{
										String feeItemsString = responseJSONObject.getString("feeItemsArray");
										JSONArray feeItemsArray = new JSONArray(feeItemsString);
										
										student_reg_no = "DMP80220201211164730";
										customerFound = sageApi.createNewSalesOrder(authorizationKeyEncoded, student_reg_no, feeItemsArray);
										
										/*for(int i=0; i<respArray.length(); i++)
										{
											JSONObject respArrayEntry = respArray.getJSONObject(i);
											System.out.println("respArrayEntry.." + respArrayEntry.toString());
											String itemCode = respArrayEntry.getString("id");// + ((new SimpleDateFormat("yyyyMMddHHmmss")).format(new Date()));
											String itemFound = sageApi.findItem(authorizationKeyEncoded, itemCode);
											if(itemFound==null)
											{
												Boolean isActive = true;
												Boolean isServiceItem = true;
												Boolean isWarehouseTracked = false;
												String itemDescription1 = respArrayEntry.getString("name");
												String itemDescription2 = respArrayEntry.getString("desc").toUpperCase();
												String itemDescription3 = "Is Tuition: " + respArrayEntry.getString("isTuition").toUpperCase();
												String newItem = sageApi.createNewItem(authorizationKeyEncoded, itemCode, 
														itemDescription1, itemDescription2, itemDescription3, 
														isActive, isWarehouseTracked, isServiceItem);
												System.out.println("Name..." + itemDescription1 + " " + itemDescription2 + "("+itemDescription3+")");
											}
										}*/
									}
								}
							//}
						}
					}
					
					
					
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	
	public static void setupSageIntegration()
	{
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

			System.out.println("Welcome to Shikola - SAGE Integration");
			System.out.println("Ensure your system is connected to the internet before you continue. Press Enter key to continue");
			String textInternetCheck = in.readLine();
			System.out.println("Please answer the questions below...");
			System.out.println("====================================");
			System.out.println("Do you have SAGE installed?");
			System.out.println("1 - Yes");
			System.out.println("2 - No");
			String text = in.readLine();
			
			if(text.equals("1"))
			{
				System.out.println("");
				System.out.println("====================================");
				System.out.println("Is your SAGE version equals or greater than 9.0?");
				System.out.println("1 - Yes");
				System.out.println("2 - No");
				String text1 = in.readLine();
				
				if(text.equals("1"))
				{
					System.out.println("");
					System.out.println("====================================");
					System.out.println("Enter your SAGE company account username");
					String sageUsername = in.readLine();
					
					System.out.println("");
					System.out.println("====================================");
					System.out.println("Enter your SAGE company account password");
					String sagePassword = in.readLine();
					

					System.out.println("");
					System.out.println("====================================");
					prefs = Preferences.userRoot().node("Shikola");
					System.out.println("Enter your Shikola developer username");
					String text3 = in.readLine();
					System.out.println("");
					System.out.println("====================================");
					System.out.println("Enter your Shikola developer password");
					String text4 = in.readLine();
					
					String baseUrl = "http://shikola.com/api/api-authenticate/" + URLEncoder.encode(text3, "UTF-8") + "/" + URLEncoder.encode(text4, "UTF-8")+"/";
					JSONObject jsObj = new JSONObject();
					String response = UtilityHelper.sendGet(baseUrl, null, jsObj);
					System.out.println("Response..." + response);
					JSONObject respUserLoggedIn = new JSONObject(response);
					if(respUserLoggedIn!=null && respUserLoggedIn.has("token") && respUserLoggedIn.getString("token")!=null)
					{
						String token = respUserLoggedIn.getString("token");
					
					

						System.out.println("");
						System.out.println("====================================");
						System.out.println("Enter your Shikola Schools Confirmation Code");
						String text2 = in.readLine();
						baseUrl = "http://shikola.com/api/school-query/" + URLEncoder.encode(text2, "UTF-8");
						jsObj = new JSONObject();
						response = UtilityHelper.sendGet(baseUrl, "token=" + token, jsObj);
						System.out.println("Response..." + response);
						JSONObject respSchoolValidationIn = new JSONObject(response);
						if(respSchoolValidationIn!=null && respSchoolValidationIn.has("status") && respSchoolValidationIn.getBoolean("status")==(true))
						{
							JSONObject school = respSchoolValidationIn.getJSONObject("school");
							String schoolName = school.getString("name");
							String preferredUrl = school.getString("preferred_url");
							String default_currency = school.getString("default_currency");
							String term_spec = school.getString("term_spec");
							String tuition_level = school.getString("tuition_level");
							String default_lang = school.getString("default_lang");
							String fee_payment_type = school.getString("fee_payment_type");
							String fee_breakdown_available = school.getBoolean("fee_breakdown_available")==true ? "1" : "0";
							

							System.out.println("");
							System.out.println("====================================");
							System.out.println("Confirm if this is your school");
							System.out.println("School Name: " + schoolName);
							System.out.println("1 - Yes");
							System.out.println("2 - No");

							String text5 = in.readLine();
							if(text5.equals("1"))
							{
								System.out.println("");
								System.out.println("====================================");
								System.out.println("How often do you want to get school fees payments from your Shikola setup?");
								System.out.println("1 - Daily");
								System.out.println("2 - Weekly");
								System.out.println("3 - Bi-Weekly");
								System.out.println("4 - Monthly");
								System.out.println("5 - Quarterly");
								String text6 = in.readLine();
								
								JSONObject job_cycles = new JSONObject();
								job_cycles.put("1", "Daily");
								job_cycles.put("2", "Weekly");
								job_cycles.put("3", "Bi-Weekly");
								job_cycles.put("4", "Monthly");
								job_cycles.put("5", "Quarterly");
								
								if(job_cycles.has(text6))
								{
									prefs.put("username", text3);
									prefs.put("password", text4);
									prefs.put("sageUsername", sageUsername);
									prefs.put("sagePassword", sagePassword);
									prefs.put("confirmationCode", text2);
									prefs.put("schoolName", schoolName);
									prefs.put("preferredUrl", preferredUrl);
									prefs.put("default_currency", default_currency);
									prefs.put("term_spec", term_spec);
									prefs.put("tuition_level", tuition_level);
									prefs.put("default_lang", default_lang);
									prefs.put("fee_payment_type", fee_payment_type);
									prefs.put("fee_breakdown_available", fee_breakdown_available);
									prefs.put("job_cycle", job_cycles.getString(text6));
									

									System.out.println("Awesome! Your SAGE integration configuration has been saved.");
									System.out.println("Validating your configuration soon...");
									System.out.println("Thank you");
								}
								else
								{
									System.out.println("Invalid selection. Please start afresh ensuring you provide valid responses");
									System.out.println("Exiting...");
								}
							}
							else
							{
								System.out.println("Start afresh and ensure you provide the valid details for your school");
								System.out.println("Exiting...");
							}
						}
						else
						{
							System.out.println("Invalid school confirmation code. We could not verify the confirmation code provided to you.");
							System.out.println("Exiting...");
						}
					}
					else
					{
						System.out.println("Invalid username/password provided. We could not verify your account.");
						System.out.println("Exiting...");
						
					}
				}
				else if(text.equals("2"))
				{
					System.out.println("Your choice text1..." + text1);
					System.out.println("You need to upgrade or install SAGE version 9.0 or above");
					System.out.println("Exiting...");
				}
				else
				{
					System.out.println("Your choice text1..." + text1);
					System.out.println("Invalid option");
					System.out.println("Exiting...");
				}
			}
			else if(text.equals("2"))
			{
				System.out.println("Your choice text..." + text);
				System.out.println("You need to upgrade or install SAGE version 9.0 or above");
				System.out.println("Exiting...");
			}
			else
			{
				System.out.println("Invalid option");
				System.out.println("Exiting...");
			}	
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

}

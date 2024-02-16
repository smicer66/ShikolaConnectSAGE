package com.probase.shikola.sage;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.probase.shikola.sage.UtilityHelper;

public class SageApi {
	public SageApi()
	{
		
	}
	
	
	public static void connectToSAGE()
	{
		String baseUrl = "";
	}
	
	
	
	
	
	
	public String findCustomer(String authorizationKeyEncoded, String customerCode) throws Exception
	{
		String baseUrl = "http://localhost:5000/freedom.core/SageOneTest/SDK/Rest/CustomerFind?callback=Json&code=" + customerCode;
		String parameters = null;
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("Authorization", "Basic QWRtaW46YWRtaW4=");
		String findCustomerResp = UtilityHelper.sendGet(baseUrl, parameters, jsonObj);
		//System.out.println("findCustomerResp.... " + findCustomerResp);
		return findCustomerResp;
	}


	public String createNewCustomer(String authorizationKeyEncoded, String student_reg_no, String first_name, String last_name,
			String other_name, Double account_balance, Boolean charge_tax, String currency, 
			String mobile_number, String email_address, String gender) throws Exception {
		// TODO Auto-generated method stub
		
		String baseUrl = "http://localhost:5000/freedom.core/SageOneTest/SDK/Rest/CustomerInsert";//?callback=Json
		String parameters = null;
		JSONObject joParent = new JSONObject();
		JSONObject jo = new JSONObject();
		jo.put("Code", student_reg_no);
		jo.put("Description", (first_name + " " + last_name));
		jo.put("Active", true);
		jo.put("ChargeTax", charge_tax);
		jo.put("AccountBalance", account_balance);
		/*jo.put("Currency", currency);*/
		jo.put("Title", gender!=null && gender.equals("MALE") ? "Mr" : "Ms");
		jo.put("IDNumber", student_reg_no);
		if(other_name!=null && other_name.length()>0)
			jo.put("Initials", (other_name.toUpperCase().charAt(0)));
		jo.put("DeliverTo", (first_name + " " + last_name));
		jo.put("Telephone", mobile_number);
		jo.put("EmailAddress", email_address);
		jo.put("CheckTerms", false);
		jo.put("UseEmail", true);
		jo.put("StatementZipPassword", student_reg_no);/**/
		joParent.put("client", jo);
		
		
		parameters = joParent.toString();
		System.out.println("parameters..." + parameters);
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("Authorization", "Basic QWRtaW46YWRtaW4=");
		jsonObj.put("Content-Type", "application/json");
		jsonObj.put("Accept", "application/json");
		String findCustomerResp = UtilityHelper.sendPost(baseUrl, parameters, jsonObj);
		System.out.println("createNewCustomer.... " + findCustomerResp);
		return findCustomerResp;
	}
	
	
	
	public String createNewSalesOrder(String authorizationKeyEncoded, String student_reg_no, JSONArray feeItemsArray) throws Exception {
		// TODO Auto-generated method stub
		
		String baseUrl = "http://localhost:5000/freedom.core/SageOneTest/SDK/Rest/SalesOrderPlaceOrder";//?callback=Json
		String parameters = null;
		JSONObject joParent = new JSONObject();
		JSONObject jo = new JSONObject();
		jo.put("CustomerAccountCode", student_reg_no);
		jo.put("OrderDate", "/Date("+ (new SimpleDateFormat("yyyy-MM-dd").format(new Date())) +")/");
		jo.put("InvoiceDate", "/Date("+ (new SimpleDateFormat("yyyy-MM-dd").format(new Date())) +")/");
		
		JSONArray documentLines = new JSONArray();
		//for(int i=0; i<feeItemsArray.length(); i++)
		//{
			JSONObject feeItemsArrayEntry = feeItemsArray.getJSONObject(0);
			String academic_year_term = feeItemsArrayEntry.getString("academic_year_term");
			String academic_year_id = feeItemsArrayEntry.getString("academic_year_id");
			String academic_term_id = feeItemsArrayEntry.getString("academic_term_id");
			String fee_item_name = feeItemsArrayEntry.getString("fee_item_name");
			String student_new_old = feeItemsArrayEntry.getString("student_new_old");
			String accommodation_type = feeItemsArrayEntry.getString("accommodation_type");
			String amountStr = feeItemsArrayEntry.getString("amount");
			Double amount = Double.valueOf(amountStr);
			String description = feeItemsArrayEntry.getString("description");
			String payment_item_id = feeItemsArrayEntry.getString("payment_item_id");
			Boolean fee_item_order = feeItemsArrayEntry.getBoolean("fee_item_order");
			
			System.out.println("payment_item_id..." + payment_item_id);
			String itemFound = findItem(authorizationKeyEncoded, payment_item_id);
			if(itemFound!=null)
			{
				System.out.println("payment_item_id..." + payment_item_id);
				System.out.println("itemFound..." + itemFound);
				String accountCode = "";
				JSONObject dl = new JSONObject();
				dl.put("StockCode", payment_item_id);
				dl.put("Quantity", 1.0);
				dl.put("ToProcess", 1.0);
				dl.put("UnitPrice", amount);
				
				documentLines.put(dl);
			}
			
			
		//}
		
		jo.put("lines", documentLines);
		jo.put("financialLines", documentLines);
		joParent.put("quote", jo);
		
		 
		parameters =  joParent.toString();
		System.out.println("parameters..." + parameters);
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("Authorization", "Basic QWRtaW46YWRtaW4=");
		jsonObj.put("Content-Type", "application/json");
		jsonObj.put("Accept", "application/json");
		String findCustomerResp = UtilityHelper.sendPost(baseUrl, parameters, jsonObj);
		System.out.println("createNewCustomer.... " + findCustomerResp);
		return findCustomerResp;
	}


	
	public String findItem(String authorizationKeyEncoded, String itemCode) throws Exception
	{
		String baseUrl = "http://localhost:5000/freedom.core/SageOneTest/SDK/Rest/InventoryItemFind?callback=Json&Code=" + itemCode;
		String parameters = null;
		JSONObject jsonObj = new JSONObject();
		System.out.println("authorizationKeyEncoded..." + authorizationKeyEncoded);
		jsonObj.put("Authorization", "Basic QWRtaW46YWRtaW4=");
		String findCustomerResp = UtilityHelper.sendGet(baseUrl, parameters, jsonObj);
		//System.out.println("findCustomerResp.... " + findCustomerResp);
		return findCustomerResp;
	}
	
	
	public String createNewItem(String authorizationKey, String itemCode, String itemDescription1,
			String itemDescription2, String itemDescription3, Boolean isActive, Boolean isWarehouseTracked, 
			Boolean isServiceItem) throws Exception {
		// TODO Auto-generated method stub
		String baseUrl = "http://localhost:5000/freedom.core/SageOneTest/SDK/Rest/InventoryItemInsert";//?callback=Json
		String parameters = null;
		JSONObject joParent = new JSONObject();
		JSONObject jo = new JSONObject();
		jo.put("Code", itemCode);
		jo.put("Description", itemDescription1);
		jo.put("Description_2", itemDescription2);
		jo.put("Description_3", itemDescription3);
		jo.put("Active", isActive);
		jo.put("IsWarehouseTracked", isWarehouseTracked);
		jo.put("IsServiceItem", isServiceItem);/**/
		joParent.put("item", jo);
		
		parameters = joParent.toString();
		System.out.println("parameters..." + parameters);
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("Authorization", "Basic " + authorizationKey);
		jsonObj.put("Content-Type", "application/json");
		jsonObj.put("Accept", "application/json");
		String findCustomerResp = UtilityHelper.sendPost(baseUrl, parameters, jsonObj);
		System.out.println("createNewCustomer.... " + findCustomerResp);
		return findCustomerResp;
	}
}

package com.dreawer.customer.web;

import com.dreawer.customer.domain.Organize;
import com.dreawer.customer.domain.User;
import com.dreawer.customer.service.OrganizeService;
import com.dreawer.customer.service.UserService;
import com.dreawer.customer.utils.MD5Utils;
import com.dreawer.customer.web.form.BaseLoginForm;
import com.dreawer.customer.web.form.UserBaseForm;
import com.dreawer.responsecode.rcdt.EntryError;
import com.dreawer.responsecode.rcdt.Error;
import com.dreawer.responsecode.rcdt.ResponseCode;
import com.dreawer.responsecode.rcdt.ResponseCodeRepository;
import com.dreawer.responsecode.rcdt.RuleError;
import com.dreawer.responsecode.rcdt.Success;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dreawer.customer.constants.ControllerConstants.REQ_LOGIN_COMMON;
import static com.dreawer.customer.constants.ControllerConstants.REQ_LOGIN_WXAPP;

@RestController
public class SignInController extends BaseController {
	
    @Autowired
	private UserService userService; // 用户信息服务
    
    @Autowired
    private OrganizeService organizeService; // 用户信息服务
    
    private Logger logger = LoggerFactory.getLogger(this.getClass());
	

    /**
     * 通用登录。
     * @param req 用户请求。
     * @param form 登录校验表单。
     * @param result 表单校验结果。
     * @return
     */
    @RequestMapping(value=REQ_LOGIN_COMMON, method=RequestMethod.POST)
    public ResponseCode login(HttpServletRequest req, 
    		@RequestBody @Valid BaseLoginForm form, BindingResult result) {
    	if (result.hasErrors()) {
            return ResponseCodeRepository.fetch(result.getFieldError().getDefaultMessage(), result.getFieldError().getField(), Error.ENTRY);
        }
		try {
			
			// 检查组织是否存在
			Organize organize = organizeService.findOrganizeByAppId(form.getAppId());
			if(organize==null) {
	            return RuleError.NON_EXISTENT("organize");
			}
			
            String flag = null;
			String check = "^([a-z0-9A-Z]+[-|_|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
	        Pattern regex = Pattern.compile(check);
	        Matcher matcher = regex.matcher(form.getLoginName());
	        if(matcher.matches()){
	        	flag = "email";
	        }else{
	        	check = "^1(3[0-9]|4[57]|5[0-35-9]|7[0135678]|8[0-9])\\d{8}$";
	 	        regex = Pattern.compile(check);
	 	        matcher = regex.matcher(form.getLoginName());
	 	        if(matcher.matches()){
	 	        	flag = "phone";
	 	        }
	        }
	        
	        User user = null;
	        if("email".equals(flag)){
				user = userService.findUserByEmail(form.getLoginName(), organize.getId());
	        }else if("phone".equals(flag)){
				user = userService.findUserByPhone(form.getLoginName(), organize.getId());
	        }else{
				return EntryError.FORMAT("loginName");
	        }
			if(user==null){
	            return RuleError.NON_EXISTENT("user");
			}
			if(!MD5Utils.encrypt(form.getPassword(), "dreawer").equals(user.getPassword())){
				return Error.BUSINESS("password");
			}
        	return Success.SUCCESS(signInUser(req, user.getId()));
		}catch(Exception e){
			 e.printStackTrace();
		     logger.error("error",e);
	         return Error.APPSERVER;
		}
    }
    
    /**
     * 小程序用户登陆。
     * @param req 用户请求。
     * @param form 登录表单。
     * @param result 表单校验结果。
     * @return 登录成功返回令牌信息。
     */
    @RequestMapping(value=REQ_LOGIN_WXAPP, method=RequestMethod.POST)
    public ResponseCode loginByWxappp(HttpServletRequest req, 
    		@RequestBody @Valid UserBaseForm form, BindingResult result) {
    	if (result.hasErrors()) {
            return ResponseCodeRepository.fetch(result.getFieldError().getDefaultMessage(), result.getFieldError().getField(), Error.ENTRY);
        }
		try {
			User user = userService.findUserById(form.getUserId());
			if(user==null) {
	            return RuleError.NON_EXISTENT("user");
			}
            Map<String, String> params = new HashMap<>();
            params.put("token", signInUser(req, user.getId()));
        	return Success.SUCCESS(params);
		}catch(Exception e){
			 e.printStackTrace();
		     logger.error("error",e);
	         return Error.APPSERVER;
		}
    }
}

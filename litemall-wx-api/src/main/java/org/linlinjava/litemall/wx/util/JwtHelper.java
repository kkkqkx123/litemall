package org.linlinjava.litemall.wx.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

@Component
public class JwtHelper {
	// 从配置文件注入JWT密钥
	@Value("${app.jwt.secret:APZvEuDRA9S2j2m9bpBKPPEiCbSRChKayBYEDc4RIK8=}")
	private String secret;
	
	// 从配置文件注入JWT配置
	@Value("${app.jwt.issuer:LITEMALL}")
	private String issuer;
	
	@Value("${app.jwt.subject:this is litemall token}")
	private String subject;
	
	@Value("${app.jwt.audience:MINIAPP}")
	private String audience;
	
	
	public String createToken(Integer userId){
		try {
		    Algorithm algorithm = Algorithm.HMAC256(secret);
		    Map<String, Object> map = new HashMap<String, Object>();
		    Date nowDate = new Date();
		    // 过期时间：2小时
		    Date expireDate = getAfterDate(nowDate,0,0,0,2,0,0);
	        map.put("alg", "HS256");
	        map.put("typ", "JWT");
		    String token = JWT.create()
		    	// 设置头部信息 Header
		    	.withHeader(map)
		    	// 设置 载荷 Payload
		    	.withClaim("userId", userId)
		        .withIssuer(issuer)
		        .withSubject(subject)
		        .withAudience(audience)
		        // 生成签名的时间 
		        .withIssuedAt(nowDate)
		        // 签名过期的时间 
		        .withExpiresAt(expireDate)
		        // 签名 Signature
		        .sign(algorithm);
		    return token;
		} catch (JWTCreationException exception){
			exception.printStackTrace();
		}
		return null;
	}
	
	public Integer verifyTokenAndGetUserId(String token) {
		try {
		    Algorithm algorithm = Algorithm.HMAC256(secret);
		    JWTVerifier verifier = JWT.require(algorithm)
		        .withIssuer(issuer)
		        .build();
		    DecodedJWT jwt = verifier.verify(token);
		    Map<String, Claim> claims = jwt.getClaims();
		    Claim claim = claims.get("userId");
		    return claim.asInt();
		} catch (JWTVerificationException exception){
//			exception.printStackTrace();
		}
		
		return 0;
	}
	
	public  Date getAfterDate(Date date, int year, int month, int day, int hour, int minute, int second){
		if(date == null){
			date = new Date();
		}
		
		Calendar cal = new GregorianCalendar();
		
		cal.setTime(date);
		if(year != 0){
			cal.add(Calendar.YEAR, year);
		}
		if(month != 0){
			cal.add(Calendar.MONTH, month);
		}
		if(day != 0){
			cal.add(Calendar.DATE, day);
		}
		if(hour != 0){
			cal.add(Calendar.HOUR_OF_DAY, hour);
		}
		if(minute != 0){
			cal.add(Calendar.MINUTE, minute);
		}
		if(second != 0){
			cal.add(Calendar.SECOND, second);
		}
		return cal.getTime();
	}
	
}

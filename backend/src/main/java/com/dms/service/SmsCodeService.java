package com.dms.service;

import cn.hutool.core.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 短信验证码服务
 * - 生成6位随机验证码并存入 Redis
 * - 验证用户提交的验证码是否正确
 * - 实际的短信发送逻辑已注释（仅测试用，验证码直接打印到控制台）
 */
@Service
public class SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeService.class);

    private final StringRedisTemplate redisTemplate;

    @Value("${dms.redis.sms-code-key-prefix}")
    private String smsCodeKeyPrefix;

    @Value("${dms.redis.sms-code-expire}")
    private long smsCodeExpire;

    public SmsCodeService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @return 生成的验证码（仅测试时返回，生产环境不应返回）
     */
    public String sendCode(String phone) {
        // 1. 生成6位随机数字验证码
        String code = RandomUtil.randomNumbers(6);

        // 2. 存入 Redis，设置过期时间
        String redisKey = smsCodeKeyPrefix + phone;
        redisTemplate.opsForValue().set(redisKey, code, smsCodeExpire, TimeUnit.SECONDS);

        // 3. 发送短信（实际短信API调用 —— 已注释，仅测试）
        // ============================================================
        // 以下为实际生产环境中的短信发送逻辑，接入阿里云/腾讯云短信服务
        // 目前处于测试阶段，验证码直接输出到控制台
        // ============================================================
        // try {
        //     // 阿里云短信示例：
        //     // SmsClient client = new SmsClient(accessKeyId, accessKeySecret);
        //     // SendSmsRequest request = new SendSmsRequest();
        //     // request.setPhoneNumbers(phone);
        //     // request.setSignName("DMS驾校");
        //     // request.setTemplateCode("SMS_XXXXXXX");
        //     // request.setTemplateParam("{\"code\":\"" + code + "\"}");
        //     // client.sendSms(request);
        //
        //     // 腾讯云短信示例：
        //     // SmsSingleSender sender = new SmsSingleSender(appId, appKey);
        //     // ArrayList<String> params = new ArrayList<>();
        //     // params.add(code);
        //     // params.add(String.valueOf(smsCodeExpire / 60)); // 过期分钟数
        //     // sender.sendWithParam("86", phone, templateId, params, smsSign, "", "");
        // } catch (Exception e) {
        //     log.error("短信发送失败, phone={}, error={}", phone, e.getMessage());
        //     throw new RuntimeException("短信发送失败，请稍后重试");
        // }

        // 测试模式：直接将验证码打印到控制台
        log.info("【DMS测试模式】手机号: {} 的验证码为: {} (有效期{}秒)", phone, code, smsCodeExpire);

        return code;
    }

    /**
     * 校验验证码
     *
     * @param phone 手机号
     * @param code  用户输入的验证码
     * @return true=验证通过, false=验证失败
     */
    public boolean verifyCode(String phone, String code) {
        String redisKey = smsCodeKeyPrefix + phone;
        String cachedCode = redisTemplate.opsForValue().get(redisKey);

        if (cachedCode != null && cachedCode.equals(code)) {
            // 验证通过后立即删除，防止重复使用
            redisTemplate.delete(redisKey);
            return true;
        }
        return false;
    }
}

package com.ruoyi.tts.controller;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.tts.dto.AuthInfo;
import com.ruoyi.tts.dto.LoginParam;
import com.ruoyi.tts.dto.SayParam;
import com.ruoyi.tts.dto.Session;
import com.ruoyi.tts.service.TtsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/tts")
public class TtsController {
    @Autowired
    private TtsService ttsService;
    @Autowired
    private RedisCache redisCache;

    @RequestMapping("/login")
    public AjaxResult login(@RequestBody LoginParam loginParam) {
        Map<String, Object> result = new HashMap<>();
        AuthInfo authInfo = ttsService.serviceAuth(loginParam.getUsername(), loginParam.getPassword());
        Session session = ttsService.loginMiAi(authInfo);
        JSONArray devices = ttsService.getDevice(session);
        if (devices.size() < 1) {
            return AjaxResult.error(201, "没有在线设备");
        }
        session.setDeviceId(devices.getJSONObject(0).getString("deviceID"));

        redisCache.setCacheObject(Constants.TTS_TOKEN_KEY + session.getDeviceId(), session);
        result.put(Constants.TOKEN, session.getDeviceId());
        result.put("deviceId", session.getDeviceId());
        return AjaxResult.success(result);
    }

    @RequestMapping("/say")
    public AjaxResult say(@RequestBody SayParam sayParam) {
        JSONObject jsonObject = ttsService.say(getSession(), sayParam.getText());
        if (jsonObject.getInteger("code") != 0) {
            return AjaxResult.error("我猜你没输入内容");
        }
        return AjaxResult.success(jsonObject);
    }

    @RequestMapping("/share")
    public AjaxResult share() {
        String token = UUID.fastUUID().toString();
        redisCache.setCacheObject(Constants.TTS_SHARE_TOKEN_KEY + token, getSession());
        return AjaxResult.success().put(Constants.TOKEN, token);
    }

    @RequestMapping("/getDevice")
    public AjaxResult getDevice() {
        JSONArray result = ttsService.getDevice(getSession());
        return AjaxResult.success(result);
    }

    // ————————————————  媒体控制  ————————————————

    @RequestMapping("/setVolume")
    public AjaxResult setVolume(Integer volume) {
        JSONObject result = ttsService.setVolume(getSession(), volume);
        return AjaxResult.success(result);
    }

    @RequestMapping("/getVolume")
    public AjaxResult getVolume() {
        JSONObject result = ttsService.getVolume(getSession());
        return AjaxResult.success(result);
    }

    @RequestMapping("/play")
    public AjaxResult play() {
        JSONObject result = ttsService.play(getSession());
        return AjaxResult.success(result);
    }

    @RequestMapping("/pause")
    public AjaxResult pause() {
        JSONObject result = ttsService.pause(getSession());
        return AjaxResult.success(result);
    }

    @RequestMapping("/togglePlayState")
    public AjaxResult togglePlayState() {
        JSONObject result = ttsService.togglePlayState(getSession());
        return AjaxResult.success(result);
    }

    @RequestMapping("/prev")
    public AjaxResult prev() {
        JSONObject result = ttsService.prev(getSession());
        return AjaxResult.success(result);
    }

    @RequestMapping("/next")
    public AjaxResult next() {
        JSONObject result = ttsService.next(getSession());
        return AjaxResult.success(result);
    }

    private Session getSession() {
        HttpServletRequest request = ServletUtils.getRequest();
        String type = request.getParameter("type");
        if ("share".equals(type)) {
            String token = request.getParameter(Constants.TOKEN);
            if (StrUtil.isNotBlank(token)) {
                Session session = redisCache.getCacheObject(Constants.TTS_SHARE_TOKEN_KEY + token);
                if (session == null) {
                    throw new ServiceException("token过期", 500);
                }
                return session;
            } else {
                throw new ServiceException("token不存在", 500);
            }
        } else {
            String token = request.getHeader(Constants.TOKEN);
            if (StrUtil.isNotBlank(token)) {
                Session session = redisCache.getCacheObject(Constants.TTS_TOKEN_KEY + token);
                if (session == null) {
                    throw new ServiceException("token过期", 401);
                }
                return session;
            } else {
                throw new ServiceException("token不存在", 401);
            }
        }
    }
}

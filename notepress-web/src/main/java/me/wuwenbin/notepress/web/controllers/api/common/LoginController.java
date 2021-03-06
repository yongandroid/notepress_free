package me.wuwenbin.notepress.web.controllers.api.common;

import cn.hutool.cache.Cache;
import com.google.code.kaptcha.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.wuwenbin.notepress.api.annotation.JwtIgnore;
import me.wuwenbin.notepress.api.model.NotePressResult;
import me.wuwenbin.notepress.api.model.entity.system.SysUser;
import me.wuwenbin.notepress.api.model.jwt.JwtHelper;
import me.wuwenbin.notepress.api.service.ISysUserService;
import me.wuwenbin.notepress.api.utils.NotePressIpUtils;
import me.wuwenbin.notepress.service.utils.NotePressJwtUtils;
import me.wuwenbin.notepress.service.utils.NotePressSessionUtils;
import me.wuwenbin.notepress.web.controllers.api.NotePressBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author wuwen
 */
@Slf4j
@RestController
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class LoginController extends NotePressBaseController {

    private final ISysUserService userService;
    private final JwtHelper jwtHelper;
    @Qualifier("kaptchaCodeCache")
    private final Cache<String, String> kaptchaCodeCache;


    /**
     * 后台管理/网站用户登录
     *
     * @param loginType
     * @param request
     * @param username
     * @param password
     * @param code
     * @return
     */
    @JwtIgnore
    @PostMapping("/{loginType}/login")
    public NotePressResult doLogin(@PathVariable String loginType, HttpServletRequest request,
                                   @RequestParam String username, @RequestParam String password,
                                   @RequestParam String code) {
        String googleCode = kaptchaCodeCache.get(Constants.KAPTCHA_SESSION_KEY);
        kaptchaCodeCache.clear();
        if (code == null) {
            return NotePressResult.createErrorMsg("请输入验证码");
        }
        if (!code.equalsIgnoreCase(googleCode)) {
            return NotePressResult.createErrorMsg("验证码不匹配，请刷新页面后重试！");
        }
        NotePressResult loginResult = userService.doLogin(username, password, NotePressIpUtils.getRemoteAddress(request));
        NotePressResult loginResponse;
        if (loginResult.isSuccess()) {
            SysUser sessionUser = loginResult.getDataBean(SysUser.class);
            loginResponse = writeJsonOkMsg("登录成功");

            if ("admin".equals(loginType)) {
                if (sessionUser.getAdmin()) {
                    String token = NotePressJwtUtils.createJwt(sessionUser, jwtHelper);
                    loginResponse.put("access_token", NotePressJwtUtils.TOKEN_PREFIX + token);
                    NotePressSessionUtils.setSessionUser(sessionUser, token);
                } else {
                    loginResponse = NotePressResult.createErrorMsg("非法登录！");
                }
            } else {
                String lastVisitUrl = setSessionReturnLastVisitUrl(sessionUser, null);
                loginResponse.addExtra("url", lastVisitUrl);
                NotePressSessionUtils.setSessionUser(sessionUser, null);
            }
        } else {
            loginResponse = NotePressResult.createErrorMsg(loginResult.getMsg());
        }
        return loginResponse;
    }

    /**
     * 注销/退出登录
     *
     * @param logoutType
     * @return
     */
    @GetMapping("/{logoutType}/logout")
    public NotePressResult logout(@PathVariable String logoutType) {
        log.info("登录类型：{}，【{}】退出登录", logoutType, NotePressSessionUtils.getSessionUser().getUsername());
        NotePressSessionUtils.invalidSessionUser();
        return NotePressResult.createOkMsg("退出成功！");
    }
}

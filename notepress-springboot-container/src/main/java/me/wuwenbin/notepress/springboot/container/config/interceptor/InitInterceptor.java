package me.wuwenbin.notepress.springboot.container.config.interceptor;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import me.wuwenbin.notepress.api.model.NotePressResult;
import me.wuwenbin.notepress.api.service.IParamService;
import me.wuwenbin.notepress.api.utils.NotePressUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * created by Wuwenbin on 2019/11/26 at 5:29 下午
 *
 * @author wuwenbin
 */
@Component
public class InitInterceptor implements HandlerInterceptor {

    private final IParamService paramService = NotePressUtils.getBean(IParamService.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        NotePressResult initStatusRr = paramService.fetchInitStatus();
        boolean status = initStatusRr.getBoolData();
        if (!status) {
            JSONObject jsonObject = JSONUtil.createObj();
            jsonObject.putAll(NotePressResult.createErrorMsg("您还未进行初始化设置！"));
            response.setHeader("Content-Type", "application/json");
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(jsonObject.toString());
            return false;
        }
        return true;
    }
}

package com.airticket.util;

import com.airticket.config.ApiException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class CurrentUser {
    private CurrentUser() {}
    public static Integer id() {
        ServletRequestAttributes attributes=(ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        Object id=attributes == null ? null : attributes.getRequest().getAttribute("principalId");
        if (id == null) throw new ApiException(401,"请先登录");
        return ((Number)id).intValue();
    }
}

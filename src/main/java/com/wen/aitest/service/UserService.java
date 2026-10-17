package com.wen.aitest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wen.aitest.common.BizException;
import com.wen.aitest.dto.LoginRequest;
import com.wen.aitest.dto.LoginResponse;
import com.wen.aitest.dto.RegisterRequest;
import com.wen.aitest.entity.User;
import com.wen.aitest.mapper.UserMapper;
import com.wen.aitest.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/** 用户业务：注册与登录 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** 注册：判重 → BCrypt 加密 → 组装 → 入库 */
    public void register(RegisterRequest req) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (count > 0) {
            throw new BizException(1002, "用户名已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() == null ? req.getUsername() : req.getNickname());
        user.setStatus(1);

        userMapper.insert(user);
    }

    /**
     * 登录：查用户 → BCrypt 比对密码 → 检查状态 → 签发 token。
     * 查无此人与密码错误抛同一提示，防止接口被用来枚举已注册用户名。
     */
    public LoginResponse login(LoginRequest req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null) {
            throw new BizException(1003, "用户名或密码错误");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException(1003, "用户名或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BizException(1004, "账号已禁用");
        }

        String token = jwtUtil.generate(user.getUsername());
        return new LoginResponse(token, user.getUsername(), user.getNickname());
    }
}

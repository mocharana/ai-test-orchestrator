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

/**
 * 用户业务：注册 / 登录。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;   // 新增：签发门票要用

    /**
     * 注册四步：判重 → 加密 → 组装 → 入库（M1-2 已讲）
     */
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
     * 登录四步（调用侧真实顺序）：
     * 1. 按用户名锁定那一行 —— "拿谁的密文"就看这步（你推出来的）
     * 2. matches 比密码：库里密文取盐重算，密文 vs 密文（明文永不直接上场）
     * 3. 查账号状态（软禁用）
     * 4. 签发 JWT 门票
     *
     * 注意 1、2 失败抛同一个提示"用户名或密码错误"——不区分到底错哪个，
     * 防黑客用接口枚举"哪些用户名注册过"（撞库前奏）。
     */
    public LoginResponse login(LoginRequest req) {
        // 1. 查人
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null) {
            throw new BizException(1003, "用户名或密码错误");
        }

        // 2. 比密码（matches 内部：取盐 → 重算 → 比密文）
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException(1003, "用户名或密码错误");
        }

        // 3. 查状态
        if (user.getStatus() == 0) {
            throw new BizException(1004, "账号已禁用");
        }

        // 4. 签发门票
        String token = jwtUtil.generate(user.getUsername());
        return new LoginResponse(token, user.getUsername(), user.getNickname());
    }
}

package fu.mss301.user.service;

import fu.mss301.user.dto.LoginRequestDTO;
import fu.mss301.user.dto.RegisterRequestDTO;
import fu.mss301.user.dto.UserDTO;
import fu.mss301.user.entity.User;
import fu.mss301.user.repository.UserRepository;
import fu.mss301.user.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public List<UserDTO> findAll() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<UserDTO> findById(Long id) {
        return userRepository.findById(id).map(this::toDTO);
    }

    public UserDTO create(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists: " + user.getEmail());
        }
        // BỔ SUNG: Mã hóa mật khẩu nếu có truyền vào, mặc định role CUSTOMER
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            throw new RuntimeException("Password is required");
        }
        if (user.getRole() == null) user.setRole("CUSTOMER");
        if (user.getActive() == null) user.setActive(true);

        return toDTO(userRepository.save(user));
    }

    public UserDTO update(Long id, User user) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        existing.setName(user.getName());
        existing.setEmail(user.getEmail());
        existing.setPhone(user.getPhone());
        existing.setAddress(user.getAddress());

        // BỔ SUNG: Cho phép Admin cập nhật trạng thái Ban/Unban
        if (user.getActive() != null) {
            existing.setActive(user.getActive());
        }
        // BỔ SUNG: Cho phép cập nhật Role (nếu cần)
        if (user.getRole() != null) {
            existing.setRole(user.getRole());
        }

        return toDTO(userRepository.save(existing));
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    // Hàm Register dành cho khách hàng tự đăng ký
    public UserDTO register(RegisterRequestDTO req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already exists: " + req.getEmail());
        }
        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setAddress(req.getAddress());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole("CUSTOMER");
        user.setActive(true); // Mặc định active
        return toDTO(userRepository.save(user));
    }

    // Hàm Login
    public String login(LoginRequestDTO req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // BỔ SUNG: Kiểm tra tài khoản có bị khóa không
        if (user.getActive() != null && !user.getActive()) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }
        return jwtService.generateToken(user.getUserId(), user.getEmail(), user.getRole());
    }

    private UserDTO toDTO(User u) {
        // BỔ SUNG: Trả về role và active
        return new UserDTO(u.getUserId(), u.getName(), u.getEmail(), u.getPhone(), u.getAddress(), u.getRole(), u.getActive());
    }
}
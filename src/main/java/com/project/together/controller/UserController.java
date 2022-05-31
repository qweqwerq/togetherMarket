package com.project.together.controller;

import com.project.together.VO.UserVO;
import com.project.together.config.auth.PrincipalDetails;
import com.project.together.entity.*;
import com.project.together.repository.UserMapper;
import com.project.together.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.*;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    private final LoginService loginService;

    private final BCryptPasswordEncoder passwordEncoder;

    private final CertificationService certificationService;
    //Mybatis Mapper
    private final UserMapper userMapper;

    private final RecentService recentService;
    private final ItemService itemService;
    private final FileService filesService;
    private final WishService wishService;

    @GetMapping("/users/new")
    public String createForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        log.info("회원가입 페이지");
        return "users/createUserForm";
    }

    @PostMapping("/users/new")
    public String create(@Valid UserForm form, BindingResult result, Model model) {

        if(result.hasErrors()) {
            return "users/createUserForm";
        }

        if(!userService.findByPhone(form.getUserPhone()).isEmpty()) {
            return "users/rejectForm";//이미 등록된 전화번호
        }
        //try{
        User user = new User();
        Address address = new Address();
        user.setUserId(form.getUserId());
        user.setUserPw(passwordEncoder.encode(form.getUserPw()));
        user.setUserName(form.getUserName());
        user.setUserPhone(form.getUserPhone());
        user.setCreatedAt(LocalDateTime.now());
        address.setCity(form.getCity());
        address.setStreet(form.getStreet());
        address.setZipcode(form.getZipcode());
        address.setLat(form.getLat());
        address.setLon(form.getLon());
        user.setAddress(address);
        log.info(form.getUserId());//회원가입시 아이디가 admim이면 관리자 권한으로 회원가입 시켰습니다.
        if(form.getUserId().equals("admin"))
            user.setRole("ROLE_ADMIN");
        else {
            user.setRole("ROLE_USER");
        }
        userService.join(user);
        log.info("회원가입 성공");
        return "redirect:/";
        /*} catch (IllegalStateException is) {
            return "items/rejectForm";
        }*/

    }

    @GetMapping("/QRdeal/createKakaoQRForm")
    public String createKakaoQR() { //자신의 카카오
        return "QRdeal/createKakaoQRForm";
    }

    @PostMapping("/QRdeal/createKakaoQRProc")
    public String createKakaoQRProc(@RequestParam String kakaoUrl, @AuthenticationPrincipal PrincipalDetails user) {
        User loginUser = userService.findById(user.getUsername());
        userService.setKakaoQR(loginUser.getUserIdx(), kakaoUrl);
        return "redirect:/";
    }

    @GetMapping("/QRdeal/createTossQRForm")
    public String createTossQR() { //자신의 카카오
        return "QRdeal/createTossQRForm";
    }

    @PostMapping("/QRdeal/createTossQRProc")
    public String createTossQRProc(@RequestParam String tossUrl, @AuthenticationPrincipal PrincipalDetails user) {
        User loginUser = userService.findById(user.getUsername());
        userService.setTossQR(loginUser.getUserIdx(), tossUrl);
        return "redirect:/";
    }

    @GetMapping("/QRdeal/tossQR")
    public String tossQR(Model model, @AuthenticationPrincipal PrincipalDetails user) {
        User loginUser = userService.findById(user.getUsername());

        model.addAttribute("tossQR",loginUser.getTossQr());
        return "QRdeal/tossQR";
    }

    @GetMapping("/QRdeal/kakaoQR")
    public String kakaoQR(Model model, @AuthenticationPrincipal PrincipalDetails user) {
        User loginUser = userService.findById(user.getUsername());

        model.addAttribute("kakaoQR",loginUser.getKakaoQr());
        return "QRdeal/kakaoQR";
    }

    /***
     * @Desc : 화면 호출 및 유저 정보 조회
     * 회원정보 수정 - mybatis
     */
    @GetMapping("/updateUserForm")
    public String updateUserForm(
            HttpServletRequest request, @AuthenticationPrincipal PrincipalDetails user, Model model) throws Exception{

        User loginUser = userService.findById(user.getUsername());

        UserVO userVO = new UserVO();
        userVO.setUserId(loginUser.getUserId());
        List<UserVO> userVOList = userMapper.selectUser(userVO);

        //userID 는 무조건 하나이니깐 List 0번째에서 가져오면 됨.
        model.addAttribute("user", userVOList.get(0));
        System.out.println("updateUserForm 데이터 확인 : " + userVOList.get(0).toString());

        return "users/updateUserForm";
    }

    /***
     * @Desc : 유저 정보 수정 액션
     * 회원정보 수정 - mybatis
     */
    @PostMapping("/updateUserForm")
    public String updateUserForm(@ModelAttribute UserVO userVO, Model model) throws Exception{
        System.out.println("updateUserForm 수정할 데이터 확인 : " + userVO.toString());
        List<UserVO> originUserVO = userMapper.selectUser(userVO);
//        if(!originUserVO.get(0).getUserPw().equals(userVO.getUserPw())){
//            model.addAttribute("user", originUserVO.get(0));
//            model.addAttribute("incorrectPW", "비밀번호가 틀렸습니다.");
//            return "users/updateUserForm";
//        }
        int check = userMapper.updateUser(userVO);
        System.out.println(check);
        return "redirect:/";
    }

    /***
     * @Desc : 화면 호출 및 유저 정보 조회
     * 회원정보 수정 - jpa
     */
    @GetMapping("/updateUserForm2")
    public String updateUserForm2(
            HttpServletRequest request, @AuthenticationPrincipal PrincipalDetails user, Model model) throws Exception{

        User loginUser = userService.findById(user.getUsername());

        if(loginUser.getUserPhone().length() < 5) {
            UserForm userForm = new UserForm();
            userForm.setCity(loginUser.getAddress()==null?"":loginUser.getAddress().getCity());
            userForm.setZipcode(loginUser.getAddress()==null?"":loginUser.getAddress().getZipcode());
            userForm.setStreet(loginUser.getAddress()==null?"":loginUser.getAddress().getStreet());
            model.addAttribute("userForm", userForm);

            return "users/kakaoUpdateForm";
        }
        User userInfo = loginService.login(loginUser.getUserId(), loginUser.getUserPw());
        UserForm userForm = new UserForm();
        userForm.setUserIdx(userInfo.getUserIdx());
        userForm.setUserId(userInfo.getUserId());
        userForm.setUserPw("");
        userForm.setUserName(userInfo.getUserName());
        userForm.setUserPhone(userInfo.getUserPhone());
        userForm.setCity(userInfo.getAddress()==null?"":userInfo.getAddress().getCity());
        userForm.setZipcode(userInfo.getAddress()==null?"":userInfo.getAddress().getZipcode());
        userForm.setStreet(userInfo.getAddress()==null?"":userInfo.getAddress().getStreet());
        userForm.setLon(userInfo.getAddress()==null?"":userInfo.getAddress().getLat());
        userForm.setLat(userInfo.getAddress()==null?"":userInfo.getAddress().getLon());

        //userID 는 무조건 하나이니깐 List 0번째에서 가져오면 됨.
        model.addAttribute("userForm", userForm);

        /*HttpSession session = request.getSession();                         // 세션이 있으면 있는 세션 반환, 없으면 신규 세션을 생성하여 반환
        session.setAttribute(SessionConstants.LOGIN_USER, loginUser);*/

        return "users/updateUserForm2";
    }
    @PostMapping("/updateKakaoUserForm")
    public String updateKakaoUserForm(UserForm form, @AuthenticationPrincipal PrincipalDetails user) throws Exception{

        User loginUser = userService.findById(user.getUsername());

        Address address = new Address();
        address.setCity(form.getCity());
        address.setZipcode(form.getZipcode());
        address.setStreet(form.getStreet());
        address.setLat(form.getLat());
        address.setLon(form.getLon());

        loginUser.setAddress(address);

        //update쪽에 데이터를 조회해온것에서 변경된 사항을 변경하기
        Long check = userService.update(loginUser);
        System.out.println(check);

        /*HttpSession session = request.getSession();                         // 세션이 있으면 있는 세션 반환, 없으면 신규 세션을 생성하여 반환
        session.setAttribute(SessionConstants.LOGIN_USER, user);*/

        return "redirect:/";
    }


    /***
     * @Desc : 유저 정보 수정 액션
     * 회원정보 수정 - jpa
     */
    @PostMapping("/updateUserForm2")
    public String updateUserForm2(HttpServletRequest request,
                                  UserForm form, BindingResult result, Model model) throws Exception{

        /*if(result.hasErrors()) {
            model.addAttribute("userForm", form);
            return "users/updateUserForm2";
        }*/

        User user = new User();

        user.setUserIdx(form.getUserIdx());
        user.setUserId(form.getUserId());
        user.setUserPw(passwordEncoder.encode(form.getUserPw()));
        user.setUserName(form.getUserName());
        user.setUserPhone(form.getUserPhone());
        user.setRole("ROLE_USER");

        Address address = new Address();
        address.setCity(form.getCity());
        address.setZipcode(form.getZipcode());
        address.setStreet(form.getStreet());
        address.setLat(form.getLat());
        address.setLon(form.getLon());

        user.setAddress(address);

        //update쪽에 데이터를 조회해온것에서 변경된 사항을 변경하기
        Long check = userService.update(user);
        System.out.println(check);

        /*HttpSession session = request.getSession();                         // 세션이 있으면 있는 세션 반환, 없으면 신규 세션을 생성하여 반환
        session.setAttribute(SessionConstants.LOGIN_USER, user);*/

        return "redirect:/";
    }

    /***
     * @throws
     */
    @PostMapping("/updateUserForm2_postman")
    @ResponseBody
    public String updateUserForm2_postman(@RequestBody UserVO userVO, Model model) throws Exception{
        User user = new User();

        user.setUserIdx(userVO.getUserIdx());
        user.setUserId(userVO.getUserId());
        user.setUserPw(userVO.getUserPw());
        user.setUserName(userVO.getUserName());
        user.setUserPhone(userVO.getUserPhone());

        Address address = new Address();
        address.setCity(userVO.getCity());
        address.setZipcode(userVO.getZipcode());
        address.setStreet(userVO.getStreet());

        user.setAddress(address);

        Long check = userService.update(user);
        System.out.println(check);
        return "success";
    }


    /***
     * @Desc : postman 전용, 서버개발
     * 회원정보 조회
     */
    @PostMapping("/selectUser")
    @ResponseBody
    public List<UserVO> selectUser(@RequestBody UserVO userVO) throws Exception{
        return userMapper.selectUser(userVO);
    }

    /***
     * @Desc : postman 전용, 서버개발
     * 회원정보 수정
     */
    @PostMapping("/updateUser")
    @ResponseBody
    public int updateUser(@RequestBody UserVO userVO) throws Exception{
        return userMapper.updateUser(userVO);
    }

    /***
     * @Desc : postman 전용, 서버개발
     * 회원가입
     */
    @PostMapping("/joinUser")
    @ResponseBody
    public int joinUser(@RequestBody UserVO userVO) throws Exception{
        return userMapper.joinUser(userVO);
    }

    @ResponseBody
    @RequestMapping(value="/idCheck", method=RequestMethod.POST)
    public int IdCheck(@RequestBody String memberId) throws Exception {

        int count = 0;
        if(memberId != null) count = userService.idCheck(memberId);
        return count;
    }

    @ResponseBody
    @RequestMapping(value="/idFind", method=RequestMethod.POST)
    public String findUser(String userPhone) throws Exception {
        log.info(userPhone);
        String userId = "없음";
        if(userService.findByPhone(userPhone).isEmpty()) {
            log.info("회원정보:" + userId);
            return userId;
        }
        userId = userService.findByPhone(userPhone).get(0).getUserId();
        log.info("회원정보:" + userId);
        return userId;
    }

    @GetMapping("/check/sendSMS")
    public @ResponseBody
    String sendSMS(String phoneNumber, Model model) {

        Random rand  = new Random();
        String numStr = "";
        for(int i=0; i<4; i++) {
            String ran = Integer.toString(rand.nextInt(10));
            numStr+=ran;
        }
        model.addAttribute("test", "test");
        System.out.println("수신자 번호 : " + phoneNumber);
        System.out.println("인증번호 : " + numStr);
        certificationService.certifiedPhoneNumber(phoneNumber,numStr);
        return numStr;
    }

    @GetMapping("/myPage")
    public String myPage(@AuthenticationPrincipal PrincipalDetails loginUser,//스프링 시큐리티 적용 후 세션에 담긴 유저
                         Model model){
        User user = userService.findById(loginUser.getUsername());
        model.addAttribute("user", user);
        //최근 본 상품
        List<Files> files = filesService.findAll();
        List<Item> itemList = new ArrayList<>();
        List<Recent> recentList = recentService.findByUserIdx(user.getUserIdx());
        for (Recent recent : recentList) {
            itemList.add(itemService.findOne(recent.getItemId()));
        }
        model.addAttribute("files", files);
        model.addAttribute("itemList", itemList);

        return "users/myPage";
    }


}

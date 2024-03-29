package com.joseph.petfound;

import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

@Controller
public class HomeController {

    @Autowired
    MessageRepository list;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    CloudinaryConfig cloudc;

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public String showRegistrationPage(Model model) {
        model.addAttribute("user", new User());
        return "registration";
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String processRegistrationPage(@Valid @ModelAttribute("user") User user, BindingResult result, Model model) {
        model.addAttribute("user", user);
        if (result.hasErrors()){
            return "registration";
        } else {
            user.setEnabled(true);
            user.setRoles(Arrays.asList(roleRepository.findByRole("USER")));
            userRepository.save(user);
            model.addAttribute("created",  true);
        }
        return "login";
    }

    @RequestMapping("/")
    public String homePage(Principal principal, Model model) {
        model.addAttribute("list", list.findAll());
        User user = ((CustomUserDetails)((UsernamePasswordAuthenticationToken) principal).getPrincipal()).getUser();
        model.addAttribute("user", user);
        Role role1 = roleRepository.findByRole("ADMIN");
        for (User check : role1.getUsers()) {
            if (check.getId() == user.getId()) {
                return "redirect:/admin";
            }
        }
        return "list";
    }

    @RequestMapping("/mine")
    public String myPage(Principal principal, Model model) {
        ArrayList<Message> myPostings = new ArrayList<>();
        User user = ((CustomUserDetails)((UsernamePasswordAuthenticationToken) principal).getPrincipal()).getUser();
        model.addAttribute("user", user);
        for (Message msg : list.findAll()) {
            if (msg.getSender().equals(user.getUsername())) {
                myPostings.add(msg);
            }
        }
        model.addAttribute("list", myPostings);
        return "list";
    }

    @RequestMapping("/open")
    public String openPosts(Principal principal, Model model) {
        ArrayList<Message> myPostings = new ArrayList<>();
        User user = ((CustomUserDetails)((UsernamePasswordAuthenticationToken) principal).getPrincipal()).getUser();
        model.addAttribute("user", user);
        for (Message msg : list.findAll()) {
            if (!msg.isFound()) {
                myPostings.add(msg);
            }
        }
        model.addAttribute("list", myPostings);
        return "list";
    }

    @RequestMapping("/search")
    public String search(@RequestParam(name = "search") String text, Principal principal, Model model) {
        ArrayList<Message> searchPostings = new ArrayList<>();
        User user = ((CustomUserDetails)((UsernamePasswordAuthenticationToken) principal).getPrincipal()).getUser();
        model.addAttribute("user", user);
        if (text != null) {
            for (Message msg : list.findAll()) {
                if (msg.getContent() != null && msg.getContent().toLowerCase().contains(text.toLowerCase())) {
                    searchPostings.add(msg);
                } else if (msg.getSender() != null && msg.getSender().toLowerCase().contains(text.toLowerCase())) {
                    searchPostings.add(msg);
                } else if (msg.getDate() != null && msg.getDate().toLowerCase().contains(text.toLowerCase())) {
                    searchPostings.add(msg);
                } else if (msg.getName() != null && msg.getName().toLowerCase().contains(text.toLowerCase())) {
                    searchPostings.add(msg);
                }
            }
        }
        model.addAttribute("list", searchPostings);
        return "list";
    }

    @RequestMapping("/see")
    public String seePage(Principal principal, Model model) {
        model.addAttribute("list", list.findAll());
        return "see";
    }

    @RequestMapping("/admin")
    public String adminPage(Principal principal, Model model) {
        model.addAttribute("list", list.findAll());
        User user = ((CustomUserDetails)((UsernamePasswordAuthenticationToken) principal).getPrincipal()).getUser();
        model.addAttribute("user", user);
        return "list";
    }

    @RequestMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/add")
    public String addMessage(Principal principal, Model model) {
        model.addAttribute("msg", new Message());
        User user = ((CustomUserDetails)((UsernamePasswordAuthenticationToken) principal).getPrincipal()).getUser();
        model.addAttribute("user", user);
        return "add";
    }

    @RequestMapping("/update/{id}")
    public String editTask(@PathVariable("id") long id, Principal principal, Model model) {
        model.addAttribute("msg", list.findById(id).get());
        User user = ((CustomUserDetails)((UsernamePasswordAuthenticationToken) principal).getPrincipal()).getUser();
        model.addAttribute("user", user);
        return "add";
    }

    @PostMapping("/send")
    public String sendMessage(Principal principal, @Valid Message msg, BindingResult result, @RequestParam("file")MultipartFile file) {
        if (result.hasErrors()) {
            return "redirect:/add";
        }
        if (!file.isEmpty()) {
            try {
                Map uploadResult = cloudc.upload(file.getBytes(), ObjectUtils.asMap("resourcetype", "auto"));
                    msg.setImage(uploadResult.get("url").toString());
                    String info = cloudc.createUrl(uploadResult.get("public_id").toString() + ".jpg", 50, 50, "fill");
                    String thumb = info.substring(info.indexOf("'") + 1, info.indexOf("'", info.indexOf("'") + 1));
                    msg.setThumb(thumb);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        User user = ((CustomUserDetails)((UsernamePasswordAuthenticationToken) principal).getPrincipal()).getUser();
        msg.setSender(user.getUsername());
        msg.setFound(false);
        list.save(msg);

        return "redirect:/";
    }

    @RequestMapping("/complete/{id}")
    public String completeTask(@PathVariable("id") long id) {
        Message msg = list.findById(id).get();
        msg.setFound(true);
        list.save(msg);
        return "redirect:/";
    }

    @RequestMapping("/view/{id}")
    public String viewTask(@PathVariable("id") long id, Model model) {
        model.addAttribute("msg", list.findById(id).get());
        return "show";
    }

    @RequestMapping("/delete/{id}")
    public String deleteTask(@PathVariable("id") long id, Model model) {
        list.deleteById(id);
        return "redirect:/admin";
    }
}

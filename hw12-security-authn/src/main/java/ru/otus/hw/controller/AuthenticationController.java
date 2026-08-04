package ru.otus.hw.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.otus.hw.dto.UserForm;
import ru.otus.hw.exception.UserAlreadyExistsException;
import ru.otus.hw.service.UserService;

@Controller
@RequiredArgsConstructor
public class AuthenticationController {

  private final UserService userService;

  @GetMapping("/login")
  public String loginPage() {
    return "auth/login";
  }

  @GetMapping("/register")
  public String registerPage(Model model) {
    model.addAttribute("userForm", new UserForm());
    return "auth/register";
  }


  @PostMapping("/register")
  public String processRegister(
    @Valid @ModelAttribute("userForm") UserForm form,
    BindingResult bindingResult,
    RedirectAttributes redirectAttributes
  ) {
    if (bindingResult.hasErrors()) {
      return "auth/register";
    }

    try {
      userService.create(form);
    } catch (UserAlreadyExistsException exception) {
      bindingResult.rejectValue(
        "name",
        "exception.user.already-exists",
        new Object[] {exception.getUserName()},
        null
      );
      return "auth/register";
    }


    redirectAttributes.addFlashAttribute("successMessageCode", "user.created");
    return "redirect:/login";
  }
}

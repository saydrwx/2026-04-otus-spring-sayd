package ru.otus.hw.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.otus.hw.dto.AuthorDto;
import ru.otus.hw.dto.AuthorForm;
import ru.otus.hw.mapper.AuthorMapper;
import ru.otus.hw.service.AuthorService;

@Controller
@RequiredArgsConstructor
public class AuthorController {

  private final AuthorService authorService;

  private final AuthorMapper authorMapper;

  @GetMapping("/authors")
  public String listPage(Model model) {
    model.addAttribute("authors", authorService.findAll());
    return "author/list";
  }

  @GetMapping("/authors/new")
  public String createPage(Model model) {
    model.addAttribute("authorForm", new AuthorForm());
    return "author/create";
  }

  @PostMapping("/authors")
  public String create(
      @Valid @ModelAttribute("authorForm") AuthorForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    if (bindingResult.hasErrors()) {
      return "author/create";
    }

    authorService.create(form);
    redirectAttributes.addFlashAttribute("successMessageCode", "author.created");
    return "redirect:/authors";
  }

  @GetMapping("/authors/{id}/edit")
  public String editPage(@PathVariable long id, Model model) {
    AuthorDto author = authorService.findById(id);
    model.addAttribute("authorId", id);
    model.addAttribute("authorForm", authorMapper.toForm(author));
    return "author/edit";
  }

  @PostMapping("/authors/{id}")
  public String update(
      @PathVariable long id,
      @Valid @ModelAttribute("authorForm") AuthorForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes
  ) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("authorId", id);
      return "author/edit";
    }

    authorService.update(id, form);
    redirectAttributes.addFlashAttribute("successMessageCode", "author.updated");
    return "redirect:/authors";
  }

  @PostMapping("/authors/{id}/delete")
  public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
    authorService.deleteById(id);
    redirectAttributes.addFlashAttribute("successMessageCode", "author.deleted");
    return "redirect:/authors";
  }
}

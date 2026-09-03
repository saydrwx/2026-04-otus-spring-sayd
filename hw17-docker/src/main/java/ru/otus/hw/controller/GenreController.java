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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.otus.hw.dto.GenreDto;
import ru.otus.hw.dto.GenreForm;
import ru.otus.hw.mapper.GenreMapper;
import ru.otus.hw.service.GenreService;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class GenreController {

  private final GenreService genreService;

  private final GenreMapper genreMapper;

  @GetMapping("/genres")
  public String listPage(Model model) {
    model.addAttribute("genres", genreService.findAll());
    return "genre/list";
  }

  @GetMapping("/genres/new")
  public String createPage(Model model) {
    model.addAttribute("genreForm", new GenreForm());
    return "genre/create";
  }

  @PostMapping("/genres")
  public String create(
      @Valid @ModelAttribute("genreForm") GenreForm form,
      BindingResult bindingResult,
      RedirectAttributes redirectAttributes
  ) {
    if (bindingResult.hasErrors()) {
      return "genre/create";
    }

    genreService.create(form);
    redirectAttributes.addFlashAttribute("successMessageCode", "genre.created");
    return "redirect:/genres";
  }

  @GetMapping("/genres/{id}/edit")
  public String editPage(@PathVariable long id, Model model) {
    GenreDto genre = genreService.findById(id);
    model.addAttribute("genreId", id);
    model.addAttribute("genreForm", genreMapper.toForm(genre));
    return "genre/edit";
  }

  @PostMapping("/genres/{id}")
  public String update(
      @PathVariable long id,
      @Valid @ModelAttribute("genreForm") GenreForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes
  ) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("genreId", id);
      return "genre/edit";
    }

    genreService.update(id, form);
    redirectAttributes.addFlashAttribute("successMessageCode", "genre.updated");
    return "redirect:/genres";
  }

  @PostMapping("/genres/{id}/delete")
  public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
    genreService.deleteById(id);
    redirectAttributes.addFlashAttribute("successMessageCode", "genre.deleted");
    return "redirect:/genres";
  }
}

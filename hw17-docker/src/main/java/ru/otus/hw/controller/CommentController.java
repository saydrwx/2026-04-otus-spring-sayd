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
import ru.otus.hw.dto.CommentDto;
import ru.otus.hw.dto.CommentForm;
import ru.otus.hw.mapper.CommentMapper;
import ru.otus.hw.service.BookService;
import ru.otus.hw.service.CommentService;

@Controller
@RequestMapping
@RequiredArgsConstructor
public class CommentController {

  private final CommentService commentService;

  private final BookService bookService;

  private final CommentMapper commentMapper;

  @GetMapping("/books/{bookId}/comments/new")
  public String createPage(@PathVariable long bookId, Model model) {
    model.addAttribute("book", bookService.findById(bookId));
    model.addAttribute("commentForm", new CommentForm());
    return "comment/create";
  }

  @PostMapping("/books/{bookId}/comments")
  public String create(
      @PathVariable long bookId,
      @Valid @ModelAttribute("commentForm") CommentForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes
  ) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("book", bookService.findById(bookId));
      return "comment/create";
    }

    commentService.create(bookId, form);
    redirectAttributes.addFlashAttribute("successMessageCode", "comment.created");
    return "redirect:/books/" + bookId;
  }

  @GetMapping("/books/{bookId}/comments/{commentId}/edit")
  public String editPage(
      @PathVariable long bookId,
      @PathVariable long commentId,
      Model model
  ) {
    CommentDto comment = commentService.findById(commentId);
    model.addAttribute("book", bookService.findById(bookId));
    model.addAttribute("commentId", commentId);
    model.addAttribute("commentForm", commentMapper.toForm(comment));
    return "comment/edit";
  }

  @PostMapping("/books/{bookId}/comments/{commentId}")
  public String update(
      @PathVariable long bookId,
      @PathVariable long commentId,
      @Valid @ModelAttribute("commentForm") CommentForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes
  ) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("book", bookService.findById(bookId));
      model.addAttribute("commentId", commentId);
      return "comment/edit";
    }

    commentService.update(commentId, form);
    redirectAttributes.addFlashAttribute("successMessageCode", "comment.updated");
    return "redirect:/books/" + bookId;
  }

  @PostMapping("/books/{bookId}/comments/{commentId}/delete")
  public String delete(
      @PathVariable long bookId,
      @PathVariable long commentId,
      RedirectAttributes redirectAttributes
  ) {
    commentService.deleteById(commentId);
    redirectAttributes.addFlashAttribute("successMessageCode", "comment.deleted");
    return "redirect:/books/" + bookId;
  }
}

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
import ru.otus.hw.dto.BookDto;
import ru.otus.hw.dto.BookForm;
import ru.otus.hw.mapper.BookMapper;
import ru.otus.hw.service.AuthorService;
import ru.otus.hw.service.BookService;
import ru.otus.hw.service.CommentService;
import ru.otus.hw.service.GenreService;

@Controller
@RequiredArgsConstructor
public class BookController {

  private final BookService bookService;

  private final AuthorService authorService;

  private final GenreService genreService;

  private final CommentService commentService;

  private final BookMapper bookMapper;

  @GetMapping("/books")
  public String listPage(Model model) {
    model.addAttribute("books", bookService.findAll());
    return "book/list";
  }

  @GetMapping("/books/{id}")
  public String detailsPage(@PathVariable long id, Model model) {
    model.addAttribute("book", bookService.findById(id));
    model.addAttribute("comments", commentService.findAllByBookId(id));
    return "book/details";
  }

  @GetMapping("/books/new")
  public String createPage(Model model) {
    model.addAttribute("bookForm", new BookForm());
    addFormOptions(model);
    return "book/create";
  }

  @PostMapping("/books")
  public String create(
      @Valid @ModelAttribute("bookForm") BookForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes
  ) {
    if (bindingResult.hasErrors()) {
      addFormOptions(model);
      return "book/create";
    }

    BookDto book = bookService.create(form);
    redirectAttributes.addFlashAttribute("successMessageCode", "book.created");
    return "redirect:/books/" + book.id();
  }

  @GetMapping("/books/{id}/edit")
  public String editPage(@PathVariable long id, Model model) {
    BookDto book = bookService.findById(id);
    model.addAttribute("bookId", id);
    model.addAttribute("bookForm", bookMapper.toForm(book));
    addFormOptions(model);
    return "book/edit";
  }

  @PostMapping("/books/{id}")
  public String update(
      @PathVariable long id,
      @Valid @ModelAttribute("bookForm") BookForm form,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes
  ) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("bookId", id);
      addFormOptions(model);
      return "book/edit";
    }

    bookService.update(id, form);
    redirectAttributes.addFlashAttribute("successMessageCode", "book.updated");
    return "redirect:/books/" + id;
  }

  @PostMapping("/books/{id}/delete")
  public String delete(@PathVariable long id, RedirectAttributes redirectAttributes) {
    bookService.deleteById(id);
    redirectAttributes.addFlashAttribute("successMessageCode", "book.deleted");
    return "redirect:/books";
  }

  private void addFormOptions(Model model) {
    model.addAttribute("authors", authorService.findAll());
    model.addAttribute("genres", genreService.findAll());
  }
}

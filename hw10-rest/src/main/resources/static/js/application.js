'use strict';

const app = document.getElementById('app');
const alerts = document.getElementById('alerts');
let flashMessage = '';

class ApiError extends Error {
  constructor(message, errors = {}) {
    super(message);
    this.errors = errors;
  }
}

async function api(url, options = {}) {
  const headers = {Accept: 'application/json', ...options.headers};
  if (options.body) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(url, {...options, headers});
  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type') || '';
  const body = contentType.includes('json') ? await response.json() : null;
  if (!response.ok) {
    throw new ApiError(body?.detail || `Ошибка запроса: статус ${response.status}`,
        body?.errors);
  }
  return body;
}

function escapeHtml(value) {
  return String(value ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
}

function showAlert(message, type = 'success') {
  alerts.innerHTML = '';
  if (!message) {
    return;
  }
  const alert = document.createElement('div');
  alert.className = `alert alert-${type}`;
  alert.setAttribute('role', 'alert');
  alert.textContent = message;
  alerts.append(alert);
}

function navigate(hash, message = '') {
  flashMessage = message;
  if (location.hash === hash) {
    render();
  } else {
    location.hash = hash;
  }
}

function setActiveNavigation(section) {
  document.querySelectorAll('[data-nav]').forEach(link => {
    link.classList.toggle('active', link.dataset.nav === section);
    link.classList.toggle('fw-semibold', link.dataset.nav === section);
  });
}

function loading() {
  app.innerHTML = `
    <div class="text-center text-secondary py-5">
      <div class="spinner-border" role="status"></div>
      <div class="mt-2">Загрузка…</div>
    </div>`;
}

function renderFailure(error) {
  showAlert(error.message || 'Непредвиденная ошибка', 'danger');
  app.innerHTML = `
    <div class="card border-0 shadow-sm">
      <div class="card-body text-center py-5">
        <h1 class="h4">Не удалось загрузить эту страницу</h1>
        <a class="btn btn-outline-secondary mt-2" href="#/">На главную</a>
      </div>
    </div>`;
}

function displayFormErrors(form, error) {
  form.querySelectorAll('.is-invalid').forEach(field => field.classList.remove('is-invalid'));
  form.querySelectorAll('[data-error-for]').forEach(element => element.textContent = '');

  Object.entries(error.errors || {}).forEach(([fieldName, message]) => {
    const field = form.elements.namedItem(fieldName);
    if (field) {
      field.classList.add('is-invalid');
      const feedback = form.querySelector(`[data-error-for="${fieldName}"]`);
      if (feedback) {
        feedback.textContent = message;
      }
    }
  });
  showAlert(error.message, 'danger');
}

function homePage() {
  document.title = 'Библиотека';
  setActiveNavigation('');
  app.innerHTML = `
    <div class="row g-4">
      ${navigationCard('books', 'book', 'Книги',
          'Просматривайте книги и комментарии к ним и управляйте ими.')}
      ${navigationCard('authors', 'person-lines-fill', 'Авторы',
          'Просматривайте авторов, представленных в каталоге, и управляйте ими.')}
      ${navigationCard('genres', 'tags', 'Жанры',
          'Управляйте жанрами, используемыми для организации книг.')}
    </div>`;
}

function navigationCard(path, icon, title, description) {
  return `
    <div class="col-md-4">
      <a class="text-decoration-none text-body" href="#/${path}">
        <article class="card navigation-card h-100 border-0 shadow-sm">
          <div class="card-body p-4">
            <div class="feature-icon mb-3"><i class="bi bi-${icon}"></i></div>
            <h1 class="h4 card-title">${title}</h1>
            <p class="card-text text-secondary mb-0">${description}</p>
          </div>
        </article>
      </a>
    </div>`;
}

const simpleEntities = {
  authors: {
    singular: 'автора',
    title: 'Авторы',
    property: 'fullName',
    label: 'Полное имя',
    emptyMessage: 'Авторы не найдены.',
    createHeading: 'Добавление автора',
    editHeading: 'Редактирование автора',
    createdMessage: 'Автор успешно добавлен.',
    updatedMessage: 'Автор успешно изменён.',
    deletedMessage: 'Автор и все связанные с ним книги успешно удалены.',
    maxLength: 255
  },
  genres: {
    singular: 'жанр',
    title: 'Жанры',
    property: 'name',
    label: 'Название',
    emptyMessage: 'Жанры не найдены.',
    createHeading: 'Добавление жанра',
    editHeading: 'Редактирование жанра',
    createdMessage: 'Жанр успешно добавлен.',
    updatedMessage: 'Жанр успешно изменён.',
    deletedMessage: 'Жанр успешно удалён.',
    maxLength: 255
  }
};

async function simpleEntityList(kind) {
  const config = simpleEntities[kind];
  const items = await api(`/api/${kind}`);
  document.title = `${config.title} — Библиотека`;
  setActiveNavigation(kind);

  const rows = items.map(item => `
    <tr>
      <td>${item.id}</td>
      <td class="fw-semibold">${escapeHtml(item[config.property])}</td>
      <td class="text-end">
        <div class="d-inline-flex gap-2">
          <a class="btn btn-sm btn-outline-primary" href="#/${kind}/${item.id}/edit">
            <i class="bi bi-pencil me-1"></i>Изменить
          </a>
          <button class="btn btn-sm btn-outline-danger" type="button"
                  data-delete-url="/api/${kind}/${item.id}"
                  data-delete-label="${escapeHtml(config.singular)}"
                  data-delete-message="${escapeHtml(config.deletedMessage)}"
                  data-return-to="#/${kind}">
            <i class="bi bi-trash me-1"></i>Удалить
          </button>
        </div>
      </td>
    </tr>`).join('');

  app.innerHTML = `
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h1 class="h2 mb-0">${config.title}</h1>
      <a class="btn btn-primary" href="#/${kind}/new">
        <i class="bi bi-plus-lg me-1"></i>Добавить ${config.singular}
      </a>
    </div>
    <div class="card border-0 shadow-sm">
      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light"><tr>
            <th>ID</th><th>${config.label}</th>
            <th class="text-end actions-column">Действия</th>
          </tr></thead>
          <tbody>${rows || `<tr><td colspan="3" class="empty-row">${config.emptyMessage}</td></tr>`}</tbody>
        </table>
      </div>
    </div>`;
}

async function simpleEntityForm(kind, id) {
  const config = simpleEntities[kind];
  const editing = Boolean(id);
  const item = editing ? await api(`/api/${kind}/${id}`) : null;
  const heading = editing ? config.editHeading : config.createHeading;
  document.title = `${heading} — Библиотека`;
  setActiveNavigation(kind);

  app.innerHTML = `
    <div class="row justify-content-center"><div class="col-lg-8">
      <div class="card border-0 shadow-sm"><div class="card-body p-4">
        <h1 class="h3 mb-4">${heading}</h1>
        <form id="entity-form" novalidate>
          <div class="mb-4">
            <label class="form-label" for="${config.property}">${config.label}</label>
            <input class="form-control" id="${config.property}" name="${config.property}"
                   maxlength="${config.maxLength}" value="${escapeHtml(item?.[config.property])}"
                   autofocus>
            <div class="invalid-feedback" data-error-for="${config.property}"></div>
          </div>
          <button class="btn btn-primary" type="submit">
            <i class="bi bi-check-lg me-1"></i>${editing ? 'Сохранить' : 'Добавить'}
          </button>
          <a class="btn btn-outline-secondary ms-1" href="#/${kind}">Отмена</a>
        </form>
      </div></div>
    </div></div>`;

  document.getElementById('entity-form').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    try {
      await api(editing ? `/api/${kind}/${id}` : `/api/${kind}`, {
        method: editing ? 'PUT' : 'POST',
        body: JSON.stringify({[config.property]: form.elements[config.property].value})
      });
      navigate(`#/${kind}`, editing ? config.updatedMessage : config.createdMessage);
    } catch (error) {
      displayFormErrors(form, error);
    }
  });
}

async function bookList() {
  const books = await api('/api/books');
  document.title = 'Книги — Библиотека';
  setActiveNavigation('books');
  const rows = books.map(book => `
    <tr>
      <td>${book.id}</td>
      <td><a class="fw-semibold text-decoration-none" href="#/books/${book.id}">
        ${escapeHtml(book.title)}</a></td>
      <td>${escapeHtml(book.author.fullName)}</td>
      <td>${book.genres.map(genre => genreBadge(genre.name)).join('')}</td>
      <td class="text-end"><div class="d-inline-flex gap-2">
        <a class="btn btn-sm btn-outline-secondary" href="#/books/${book.id}">
          <i class="bi bi-eye me-1"></i>Открыть</a>
        <a class="btn btn-sm btn-outline-primary" href="#/books/${book.id}/edit">
          <i class="bi bi-pencil me-1"></i>Изменить</a>
        <button class="btn btn-sm btn-outline-danger" type="button"
                data-delete-url="/api/books/${book.id}" data-delete-label="книгу"
                data-delete-message="Книга и все связанные с ней комментарии успешно удалены."
                data-return-to="#/books"><i class="bi bi-trash me-1"></i>Удалить</button>
      </div></td>
    </tr>`).join('');

  app.innerHTML = `
    <div class="d-flex justify-content-between align-items-center mb-4">
      <h1 class="h2 mb-0">Книги</h1>
      <a class="btn btn-primary" href="#/books/new">
        <i class="bi bi-plus-lg me-1"></i>Добавить книгу</a>
    </div>
    <div class="card border-0 shadow-sm"><div class="table-responsive">
      <table class="table table-hover align-middle mb-0">
        <thead class="table-light"><tr><th>ID</th><th>Название</th><th>Автор</th>
          <th>Жанры</th><th class="text-end actions-column">Действия</th></tr></thead>
        <tbody>${rows || '<tr><td colspan="5" class="empty-row">Книги не найдены.</td></tr>'}</tbody>
      </table>
    </div></div>`;
}

function genreBadge(name) {
  return `<span class="badge text-bg-secondary me-1">${escapeHtml(name)}</span>`;
}

async function bookForm(id) {
  const editing = Boolean(id);
  const [authors, genres, book] = await Promise.all([
    api('/api/authors'),
    api('/api/genres'),
    editing ? api(`/api/books/${id}`) : Promise.resolve(null)
  ]);
  const heading = editing ? 'Редактирование книги' : 'Добавление книги';
  const selectedGenres = new Set(book?.genres.map(genre => genre.id) || []);
  document.title = `${heading} — Библиотека`;
  setActiveNavigation('books');

  app.innerHTML = `
    <div class="row justify-content-center"><div class="col-lg-8">
      <div class="card border-0 shadow-sm"><div class="card-body p-4">
        <h1 class="h3 mb-4">${heading}</h1>
        <form id="book-form" novalidate>
          <div class="mb-3">
            <label class="form-label" for="title">Название</label>
            <input class="form-control" id="title" name="title" maxlength="255"
                   value="${escapeHtml(book?.title)}" autofocus>
            <div class="invalid-feedback" data-error-for="title"></div>
          </div>
          <div class="mb-3">
            <label class="form-label" for="authorId">Автор</label>
            <select class="form-select" id="authorId" name="authorId">
              <option value="">Выберите автора</option>
              ${authors.map(author => `<option value="${author.id}" `
                  + `${book?.author.id === author.id ? 'selected' : ''}>`
                  + `${escapeHtml(author.fullName)}</option>`).join('')}
            </select>
            <div class="invalid-feedback" data-error-for="authorId"></div>
          </div>
          <div class="mb-4">
            <label class="form-label" for="genreIds">Жанры</label>
            <select class="form-select" id="genreIds" name="genreIds" multiple size="8">
              ${genres.map(genre => `<option value="${genre.id}" `
                  + `${selectedGenres.has(genre.id) ? 'selected' : ''}>`
                  + `${escapeHtml(genre.name)}</option>`).join('')}
            </select>
            <div class="form-text">Удерживайте Ctrl (Windows/Linux) или Command (macOS),
              чтобы выбрать несколько жанров.</div>
            <div class="invalid-feedback" data-error-for="genreIds"></div>
          </div>
          <button class="btn btn-primary" type="submit">
            <i class="bi bi-check-lg me-1"></i>${editing ? 'Сохранить' : 'Добавить'}
          </button>
          <a class="btn btn-outline-secondary ms-1" href="${editing ? `#/books/${id}` : '#/books'}">
            Отмена</a>
        </form>
      </div></div>
    </div></div>`;

  document.getElementById('book-form').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const authorValue = form.elements.authorId.value;
    const payload = {
      title: form.elements.title.value,
      authorId: authorValue ? Number(authorValue) : null,
      genreIds: Array.from(form.elements.genreIds.selectedOptions, option => Number(option.value))
    };
    try {
      const saved = await api(editing ? `/api/books/${id}` : '/api/books', {
        method: editing ? 'PUT' : 'POST',
        body: JSON.stringify(payload)
      });
      navigate(`#/books/${saved.id}`,
          editing ? 'Книга успешно изменена.' : 'Книга успешно добавлена.');
    } catch (error) {
      displayFormErrors(form, error);
    }
  });
}

async function bookDetails(id) {
  const [book, comments] = await Promise.all([
    api(`/api/books/${id}`),
    api(`/api/books/${id}/comments`)
  ]);
  document.title = `${book.title} — Библиотека`;
  setActiveNavigation('books');
  const commentCards = comments.map(comment => `
    <article class="card border-0 shadow-sm">
      <div class="card-body"><div class="d-flex justify-content-between gap-3">
        <p class="comment-text mb-0">${escapeHtml(comment.text)}</p>
        <div class="d-flex gap-2 flex-shrink-0">
          <a class="btn btn-sm btn-outline-primary"
             href="#/books/${book.id}/comments/${comment.id}/edit">
            <i class="bi bi-pencil"></i><span class="visually-hidden">Изменить</span></a>
          <button class="btn btn-sm btn-outline-danger" type="button"
                  data-delete-url="/api/comments/${comment.id}" data-delete-label="комментарий"
                  data-delete-message="Комментарий успешно удалён."
                  data-return-to="#/books/${book.id}">
            <i class="bi bi-trash"></i><span class="visually-hidden">Удалить</span></button>
        </div>
      </div></div>
    </article>`).join('');

  app.innerHTML = `
    <div class="card border-0 shadow-sm mb-4"><div class="card-body p-4">
      <div class="d-flex flex-wrap justify-content-between align-items-start gap-3">
        <div><h1 class="h2 mb-2">${escapeHtml(book.title)}</h1>
          <p class="text-secondary mb-2">Автор:
            <span class="fw-semibold">${escapeHtml(book.author.fullName)}</span></p>
          <div>${book.genres.map(genre => genreBadge(genre.name)).join('')}</div>
        </div>
        <div class="d-flex gap-2">
          <a class="btn btn-outline-primary" href="#/books/${book.id}/edit">
            <i class="bi bi-pencil me-1"></i>Изменить</a>
          <button class="btn btn-outline-danger" type="button"
                  data-delete-url="/api/books/${book.id}" data-delete-label="книгу"
                  data-delete-message="Книга и все связанные с ней комментарии успешно удалены."
                  data-return-to="#/books"><i class="bi bi-trash me-1"></i>Удалить</button>
        </div>
      </div>
    </div></div>
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h2 class="h4 mb-0">Комментарии</h2>
      <a class="btn btn-primary btn-sm" href="#/books/${book.id}/comments/new">
        <i class="bi bi-plus-lg me-1"></i>Добавить комментарий</a>
    </div>
    <div class="vstack gap-3">${commentCards || `
      <div class="card border-0 shadow-sm"><div class="empty-row">Комментариев пока нет.</div></div>`}
    </div>
    <a class="btn btn-outline-secondary mt-4" href="#/books">
      <i class="bi bi-arrow-left me-1"></i>Вернуться к книгам</a>`;
}

async function commentForm(bookId, commentId) {
  const editing = Boolean(commentId);
  const [book, comment] = await Promise.all([
    api(`/api/books/${bookId}`),
    editing ? api(`/api/comments/${commentId}`) : Promise.resolve(null)
  ]);
  if (comment && comment.bookId !== Number(bookId)) {
    throw new ApiError('Этот комментарий не относится к выбранной книге.');
  }
  const heading = editing ? 'Редактирование комментария' : 'Добавление комментария';
  document.title = `${heading} — Библиотека`;
  setActiveNavigation('books');

  app.innerHTML = `
    <div class="row justify-content-center"><div class="col-lg-8">
      <div class="card border-0 shadow-sm"><div class="card-body p-4">
        <h1 class="h3 mb-2">${heading}</h1>
        <p class="text-secondary mb-4">Книга: ${escapeHtml(book.title)}</p>
        <form id="comment-form" novalidate>
          <div class="mb-4">
            <label class="form-label" for="text">Комментарий</label>
            <textarea class="form-control" id="text" name="text" rows="5"
                      maxlength="1000" autofocus>${escapeHtml(comment?.text)}</textarea>
            <div class="invalid-feedback" data-error-for="text"></div>
          </div>
          <button class="btn btn-primary" type="submit">
            <i class="bi bi-check-lg me-1"></i>${editing ? 'Сохранить' : 'Добавить'}
          </button>
          <a class="btn btn-outline-secondary ms-1" href="#/books/${bookId}">Отмена</a>
        </form>
      </div></div>
    </div></div>`;

  document.getElementById('comment-form').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    try {
      await api(editing ? `/api/comments/${commentId}` : `/api/books/${bookId}/comments`, {
        method: editing ? 'PUT' : 'POST',
        body: JSON.stringify({text: form.elements.text.value})
      });
      navigate(`#/books/${bookId}`,
          editing ? 'Комментарий успешно изменён.' : 'Комментарий успешно добавлен.');
    } catch (error) {
      displayFormErrors(form, error);
    }
  });
}

async function render() {
  showAlert('');
  loading();
  const path = (location.hash || '#/').slice(1);
  try {
    if (path === '/') {
      homePage();
    } else if (path === '/authors' || path === '/genres') {
      await simpleEntityList(path.slice(1));
    } else if (/^\/(authors|genres)\/new$/.test(path)) {
      await simpleEntityForm(path.split('/')[1]);
    } else if (/^\/(authors|genres)\/\d+\/edit$/.test(path)) {
      const [, kind, id] = path.split('/');
      await simpleEntityForm(kind, Number(id));
    } else if (path === '/books') {
      await bookList();
    } else if (path === '/books/new') {
      await bookForm();
    } else if (/^\/books\/\d+\/edit$/.test(path)) {
      await bookForm(Number(path.split('/')[2]));
    } else if (/^\/books\/\d+$/.test(path)) {
      await bookDetails(Number(path.split('/')[2]));
    } else if (/^\/books\/\d+\/comments\/new$/.test(path)) {
      await commentForm(Number(path.split('/')[2]));
    } else if (/^\/books\/\d+\/comments\/\d+\/edit$/.test(path)) {
      const parts = path.split('/');
      await commentForm(Number(parts[2]), Number(parts[4]));
    } else {
      throw new ApiError('Страница не найдена.');
    }

    if (flashMessage) {
      showAlert(flashMessage);
      flashMessage = '';
    }
  } catch (error) {
    flashMessage = '';
    renderFailure(error);
  }
}

document.addEventListener('click', async event => {
  const button = event.target.closest('[data-delete-url]');
  if (!button) {
    return;
  }
  const label = button.dataset.deleteLabel;
  if (!confirm(`Удалить ${label}?`)) {
    return;
  }
  button.disabled = true;
  try {
    await api(button.dataset.deleteUrl, {method: 'DELETE'});
    navigate(button.dataset.returnTo, button.dataset.deleteMessage);
  } catch (error) {
    button.disabled = false;
    showAlert(error.message, 'danger');
  }
});

window.addEventListener('hashchange', render);
render();

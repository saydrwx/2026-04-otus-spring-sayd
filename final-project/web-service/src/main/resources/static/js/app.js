"use strict";
// Error responses contain the same escaped Thymeleaf fragment as a successful search.
document.addEventListener("htmx:beforeSwap", function (event) {
  if (event.detail.target.id === "doctor-results" && [400, 404, 500, 503].includes(event.detail.xhr.status)) {
    event.detail.shouldSwap = true;
    event.detail.isError = false;
  }
});
function searchFailed(event) {
  if (event.detail.elt && event.detail.elt.matches("form.filters")) {
    const results = document.getElementById("doctor-results");
    if (!results) return;
    const notice = document.createElement("p");
    notice.className = "notice error";
    notice.setAttribute("role", "alert");
    notice.textContent = "Не удалось выполнить поиск. Повторите попытку.";
    results.replaceChildren(notice);
  }
}
document.addEventListener("htmx:sendError", searchFailed);
document.addEventListener("htmx:timeout", searchFailed);

/* ============================================================
   云麓论坛 - 前端增强(自包含, 无 CDN; 核心功能不依赖 JS)
   1. 暗色主题切换  2. 移动端导航折叠  3. Markdown 实时预览  4. 防连点
   ============================================================ */
(function () {
  'use strict';

  /* 1. 暗色主题: localStorage 记忆, 首次跟随系统 */
  var themeKey = 'youlu-theme';
  function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    try { localStorage.setItem(themeKey, theme); } catch (e) { /* 忽略 */ }
  }
  var savedTheme = null;
  try { savedTheme = localStorage.getItem(themeKey); } catch (e) { /* 忽略 */ }
  if (savedTheme) {
    applyTheme(savedTheme);
  } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    applyTheme('dark');
  }
  var themeBtn = document.getElementById('themeToggle');
  if (themeBtn) {
    themeBtn.addEventListener('click', function () {
      var cur = document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      applyTheme(cur);
    });
  }

  /* 2. 移动端导航折叠 */
  var navToggle = document.getElementById('navToggle');
  var navLinks = document.getElementById('navLinks');
  if (navToggle && navLinks) {
    navToggle.addEventListener('click', function () {
      navLinks.classList.toggle('open');
    });
  }

  /* 3. Markdown 实时预览: 先 HTML 转义再转译, 杜绝注入 */
  function escapeHtml(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }
  function inline(s) {
    return s
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
      .replace(/\*([^*]+)\*/g, '<em>$1</em>')
      .replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
      .replace(/~~([^~]+)~~/g, '<del>$1</del>');
  }
  function renderMd(text) {
    var lines = escapeHtml(text).split('\n');
    var html = '';
    var inCode = false, codeLines = [], inList = false, inOrderList = false, inQuote = false;
    function closeList() {
      if (inList) { html += '</ul>'; inList = false; }
      if (inOrderList) { html += '</ol>'; inOrderList = false; }
    }
    function closeQuote() { if (inQuote) { html += '</blockquote>'; inQuote = false; } }
    for (var i = 0; i < lines.length; i++) {
      var line = lines[i];
      if (/^```/.test(line)) {
        if (inCode) {
          html += '<pre><code>' + codeLines.join('\n') + '</code></pre>';
          inCode = false; codeLines = [];
        } else {
          closeList(); closeQuote(); inCode = true;
        }
        continue;
      }
      if (inCode) { codeLines.push(line); continue; }
      var m;
      if ((m = line.match(/^(#{1,4})\s+(.*)/))) {
        closeList(); closeQuote();
        var h = m[1].length;
        html += '<h' + h + '>' + inline(m[2]) + '</h' + h + '>';
      } else if (/^>\s?/.test(line)) {
        closeList();
        if (!inQuote) { html += '<blockquote>'; inQuote = true; }
        html += '<p>' + inline(line.replace(/^>\s?/, '')) + '</p>';
      } else if ((m = line.match(/^[-*+]\s+(.*)/))) {
        closeQuote();
        if (!inList) { html += '<ul>'; inList = true; }
        html += '<li>' + inline(m[1]) + '</li>';
      } else if ((m = line.match(/^\d+\.\s+(.*)/))) {
        closeQuote();
        if (!inOrderList) { html += '<ol>'; inOrderList = true; }
        html += '<li>' + inline(m[1]) + '</li>';
      } else if (/^\s*-{3,}\s*$/.test(line)) {
        closeList(); closeQuote();
        html += '<hr>';
      } else if (line.trim() === '') {
        closeList(); closeQuote();
      } else {
        closeList(); closeQuote();
        html += '<p>' + inline(line) + '</p>';
      }
    }
    if (inCode) { html += '<pre><code>' + codeLines.join('\n') + '</code></pre>'; }
    closeList(); closeQuote();
    return html;
  }
  var editor = document.getElementById('editor');
  var preview = document.getElementById('editorPreview');
  if (editor && preview) {
    var updatePreview = function () {
      var v = editor.value;
      preview.innerHTML = v.trim() ? renderMd(v) : '预览区';
    };
    editor.addEventListener('input', updatePreview);
    updatePreview();
  }

  /* 4. 表单提交后禁用提交按钮, 防连点 */
  var forms = document.querySelectorAll('form[method="post"]');
  Array.prototype.forEach.call(forms, function (f) {
    f.addEventListener('submit', function () {
      var btns = f.querySelectorAll('button[type="submit"]');
      Array.prototype.forEach.call(btns, function (b) { b.disabled = true; });
    });
  });
})();

$(function() {
  $(window).on("popstate", history_handler);
  $.each($("figure.wordcloud"), register_cloud_handler);
  var request = parse_request();
  if (request.innerpage || request.word || request.match) {
    process_request(request, false);
  }
});


function parse_request() {
  return { innerpage: get_query_variable("innerpage"),
           word: get_query_variable("word"),
           match: get_query_variable("match")};
}

function build_request_url(request) {
  var loc = window.location;
  var url = loc.protocol + "//" + loc.host + loc.pathname;
  var query = "";
  if (request.innerpage) {
    if (query == "") { query += "?"; } else { query += "&"; }
    query += "innerpage=" + request.innerpage;
  }
  if (request.word) {
    if (query == "") { query += "?"; } else { query += "&"; }
    query += "word=" + request.word;
  }
  if (request.match) {
    if (query == "") { query += "?"; } else { query += "&"; }
    query += "match=" + request.match;
  }
  return query;
}

function add_history_entry(request, title) {
  window.history.pushState(request, title, build_request_url(request));
}

function scroll_to_page() {
  //window.location.hash = "#main-menu";
}

function medium_jump(pos) {
  var player = $("#main_video")[0];
  if (player.paused) {
    player.play();
  }
  player.currentTime = pos;
  scroll_to_page();
}

function innerpage(page_id, with_history) {
  with_history = typeof with_history !== 'undefined' ? with_history : true;
  $("article.innerpage").css("display", "none");
  $("#" + page_id).css("display", "inherit");
  scroll_to_page();

  if (with_history) {
    var title = $("#" + page_id + " h3:first").innerText;
    add_history_entry({ innerpage: page_id }, "distillery - " + title);
  }
}

function glossary(part_id) {
  $("div.glossary-part").css("display", "none");
  $("#glossary-part-" + part_id).css("display", "inherit");
}

function word(word_id, with_history) {
  with_history = typeof with_history !== 'undefined' ? with_history : true;
  $("#word .innerpage").load("words/" + word_id + ".inc.html", function(text, status) {
    innerpage("word", false);
    if (with_history) {
      var title = $("#word h3:first").innerText;
      add_history_entry({ word: word_id }, title);
    }
  });
}

function match(medium_id, with_history) {
  with_history = typeof with_history !== 'undefined' ? with_history : true;
  $("#match .innerpage").load("matches/" + medium_id + ".inc.html", function(text, status) {
    innerpage("match", false);
    if (with_history) {
      var title = $("#match h3:first").innerText;
      add_history_entry({ match: medium_id }, title);
    }
  });
}

function process_request(request, with_history) {
  with_history = typeof with_history !== 'undefined' ? with_history : true;
  if (request) {
    /*
    alert("match: " + request.match + "\n" +
          "word: " + request.word + "\n" +
          "innerpage: " + request.innerpage)
    */
    if (request.match) {
      match(request.match, with_history);
      return;
    }
    if (request.word) {
      word(request.word, with_history);
      return;
    }
    if (request.innerpage) {
      innerpage(request.innerpage, with_history);
      return;
    }
  }
  var start_id = $('.innerpage[data-start="true"]')[0].id;
  if (start_id) {
    innerpage(start_id, with_history);
  }
}

var clouds = {};

function register_cloud_data(cloud_id, cloud_data) {
  clouds[cloud_id] = cloud_data;
}

function rect_contains(r, x, y) {
  return x >= r.x && x <= (r.x + r.w) && y >= r.y && y <= (r.y + r.h);
}

function get_word_id(event) {
  var id = event.data.cloud_id;
  var x = event.pageX - $(event.currentTarget).offset().left;
  var y = event.pageY - $(event.currentTarget).offset().top;
  var words = clouds[id];
  for (var i = 0; i < words.length; i++) {
    var w = words[i];
    if (rect_contains(w.r, x, y)) {
      return w.id;
    }
  }
  return null;
}

function history_handler(event) {
  var request = event.originalEvent.state;
  process_request(request, false);
}

function cloud_click_handler(event) {
  var word_id = get_word_id(event);
  if (word_id) {
    word(word_id);
  }
}

function cloud_mousemove_handler(event) {
  var img = event.currentTarget;
  var word_id = get_word_id(event);
  if (word_id) {
    img.style.cursor = "pointer";
  } else {
    img.style.cursor = "default";
  }
}

function register_cloud_handler(i, cloud_figure) {
  var id = cloud_figure.getAttribute("data-cloud-id");
  var img = $("img.wordcloud", cloud_figure)[0];
  $(img).on("click", { cloud_id: id }, cloud_click_handler);
  $(img).on("mousemove", { cloud_id: id }, cloud_mousemove_handler);
}

/**
 * http://jeffreifman.com/2006/06/26/how-can-i-get-query-string-values-from-url-in-javascript/
 */
function get_query_variable(variable) {
  var query = window.location.search.substring(1);
  var vars = query.split('&');
  for (var i = 0; i < vars.length; i++) {
    var pair = vars[i].split('=');
    if (decodeURIComponent(pair[0]) == variable) {
      return decodeURIComponent(pair[1]);
    }
  }
  return null;
}

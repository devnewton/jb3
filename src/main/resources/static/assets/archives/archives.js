function Jb3Archives() {
	$('.jb3-post-message').replaceWith(function(_, message) {
		return jb3_post_to_html.parse(message);
	});
}
new Jb3Archives();

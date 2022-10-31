function CopyToFeedbackEditor(index) {
	var sourceEditor = CKEDITOR.instances['exerciseEdit:tabs' + index + ':editor_' + index];
	var viewEditor = CKEDITOR.instances['exerciseEdit:tabs' + index	+ ':view_editor_' + index];
	if (viewEditor != null){
		viewEditor.setData(sourceEditor.getData());
	}
}

function RegisterToolbarListener(index) {
	var sourceEditor = CKEDITOR.instances['exerciseEdit:tabs' + index + ':editor_' + index];
	sourceEditor.on('afterCommandExec', handleAfterCommandExec);
//	sourceEditor.on('selectionChange', handleAfterSelectionChange);
}

function handleAfterSelectionChange(event) {
	var commandName = event.data;
}

function handleAfterCommandExec(event) {
	var commandName = event.data.name;
	switch (commandName) {
	case "bold":
	case "italic":
	case "underline":
	case "subscript":
	case "superscript":
	case "numberedlist":
	case "bulletedlist":
	case "outdent":
	case "indent":
	case "justifyleft":
	case "justifycenter":
	case "justifyright":
	case "justifyblock":
	case "table":
	case "font":
	case "fontSize":
	case "textColor":
	case "bGColor":
		event.sender.fire( 'saveSnapshot' );
		processChange();
		break;
	default:
		break;
	}
}
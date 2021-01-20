$ ->
  vm = ko.mapping.fromJS

  $img = document.getElementById('show-image')
  $dump = document.getElementById('dump')
  readURL = (input) ->
    if input.files and input.files[0]
      reader = new FileReader
      reader.onload = (e) ->
        $img.setAttribute('src', e.target.result)
      reader.readAsDataURL input.files[0]

  $('#image').change ->
    readURL this

  vm.readDoc = ->
    # !!! mrz.scanner method only accepts tag <img> !!! #
    val = mrz.scanner($img)
    $dump.innerText = "Data:\n" + val;

  ko.applyBindings {vm}
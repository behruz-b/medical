$ ->
  my.initAjax()

  Glob = window.Glob || {}

  vm = ko.mapping.fromJS
    language: Glob.language

  readURL = (input) ->
    if input.files and input.files[0]
      reader = new FileReader
      reader.onload = (e) ->
        $('#show-image').attr 'src', e.target.result

      reader.readAsDataURL input.files[0]

  $('#capture-image').change ->
    readURL this


  vm.translate = (fieldName) -> ko.computed () ->
    index = if vm.language() is 'en' then 0 else if vm.language() is 'ru' then 1 else if vm.language() is 'uz' then 2 else 3
    vm.labels[fieldName][index]

  vm.labels =
    addAnalysisResult: [
      "Add Analysis Result"
      "Добавить результат анализа"
      "Tahlil natijasini qo'shish"
    ]
    file: [
      "File"
      "Файл"
      "Fayl"
    ]
    submit: [
      "Submit"
      "Отправить"
      "Jo'natish"
    ]

  ko.applyBindings {vm}
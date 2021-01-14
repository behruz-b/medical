$ ->
  my.initAjax()

  Glob = window.Glob || {}

  vm = ko.mapping.fromJS
    language: Glob.language

  $(document).ready ->
  $('#show_hide_password a').on 'click', (event) ->
    event.preventDefault()
    if $('#show_hide_password input').attr('type') == 'text'
      $('#show_hide_password input').attr 'type', 'password'
      $('#show_hide_password i').addClass 'fa-eye-slash'
      $('#show_hide_password i').removeClass 'fa-eye'
    else if $('#show_hide_password input').attr('type') == 'password'
      $('#show_hide_password input').attr 'type', 'text'
      $('#show_hide_password i').removeClass 'fa-eye-slash'
      $('#show_hide_password i').addClass 'fa-eye'

  vm.translate = (fieldName) -> ko.computed () ->
    index = if vm.language() is 'en' then 0 else if vm.language() is 'ru' then 1 else if vm.language() is 'uz' then 2 else 3
    vm.labels[fieldName][index]

  vm.labels =
    adminPanel: [
      "Admin Panel"
      "Главная"
      "Admin boshqaruvi"
    ]
    login: [
      "Login"
      "Логин"
      "Login"
    ]
    password: [
      "Password"
      "Пароль"
      "Parol"
    ]


  ko.applyBindings {vm}
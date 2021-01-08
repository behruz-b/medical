$ ->
  my.initAjax()

  Glob = window.Glob || {}

  vm = ko.mapping.fromJS
    language: Glob.language

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
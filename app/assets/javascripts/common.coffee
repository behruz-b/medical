if !window.console
  window.console =
    log: ->
# nothing

root = exports ? this

$ ->
  if window.toastr
    toastr.options.timeOut = 10000

root.my =
  growlTimeoutSeconds: 5

  showGrowl: (observableBoolean) ->
    observableBoolean true
    setTimeout (->
      observableBoolean false
    ), my.growlTimeoutSeconds * 1000

  showSendingSucceeded: (modelView) ->
    if modelView.messageType() == "sms"
      my.showGrowl modelView.smsSucceededShown
    else
      my.showGrowl modelView.emailSucceededShown

  showSendingFailed: (modelView) ->
    if modelView.messageType() == "sms"
      my.showGrowl modelView.smsFailedShown
    else
      my.showGrowl modelView.emailFailedShown

  resetSucceededFailed: (modelView) ->
    modelView.smsFailedShown?(false)
    modelView.smsSucceededShown?(false)
    modelView.emailFailedShown?(false)
    modelView.emailSucceededShown?(false)
    yes

  initAjax: ->
    $.ajaxSetup
      type: 'POST'
      contentType: "application/json"
      dataType: 'json'
      headers:
        'Cache-Control': 'no-cache'
        'Pragma': 'no-cache'

    # if you use multiple busy-loader elements in your html, their attributes should be same
    $busyLoader = $ '.busy-loader'
    if $busyLoader.length
      urls = $busyLoader.attr('data-bl-url') # urls to watch request, watch all post requests if no url
      $sendButtons = $ $busyLoader.attr('data-bl-selector') # disable sendButtons on specified requests

      if !urls
        checkUrl = () -> yes
      else
        checkUrl = (reqUrl) ->
          for url in urls.split(',')
            if !$.trim(url) then continue
            if reqUrl.toLowerCase().indexOf(url.toLowerCase()) > -1 then return yes
          no

      $.ajaxSetup
        beforeSend: (a, b) ->
          if /post/i.test(b.type) && checkUrl(b.url)
            $busyLoader.show()
            $sendButtons.prop('disabled', yes)

            a.always () ->
              $busyLoader.hide()
              $sendButtons.prop('disabled', no)
          if $.blockUI # and !window.DISABLE_AUTO_UNBLOCK
            a.always () ->
              $.unblockUI()

#  regexpAlpha:
#    /[a-z]/i

  hasText: (s) ->
    !_.isEmpty(_.trim(s))

#  isDigits: (s, digitsCount) ->
#    (new RegExp('^\\d{' + digitsCount + '}$')).test s

  isValidPhone: (phoneNumber) ->
    /^\d{9}$/.test phoneNumber

  isValidEmail: (email) ->
    re = /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    re.test email

  isValidInt: (s) ->
    /^\d{1,}$/.test s

  isValidDecimal2: (s) ->
    /^\d{1,}\.\d{2}$/.test s

#  convertToFloat: (observables) ->
#    for observable in observables
#      if !my.hasText(observable())
#        observable null
#      else
#        observable parseFloat(observable())

# used in dashboards

  navigateToUrl: (url) ->
    a = document.createElement('a')
    if a.click
      a.setAttribute 'href', url
      a.style.display = 'none'
      document.body.appendChild a
      a.click()
    else
      window.location = url

  blockUI: ->
    $.blockUI
      message: '<img src="/assets/images/common/landing/ajax-loader.gif" />'
      css:
        width:'0'
        left: '50%'
        top: '45%'
        border: 'none'
        color: '#fff'
        opacity: '1'
        'z-index': '99'
      overlayCSS:
        backgroundColor: '#000'
        opacity: '0.3'

if $.blockUI and !window.DISABLE_AUTO_UNBLOCK
  $.ajaxSetup
    beforeSend: (a) ->
      a.always () ->
        $.unblockUI()
$ ->
  my.initAjax()

  Glob = window.Glob || {}

  vm = ko.mapping.fromJS

  !(($) ->
    'use strict'
    # Preloader
    $(window).on 'load', ->
      if $('#preloader').length
        $('#preloader').delay(100).fadeOut 'slow', ->
          $(this).remove()
          return
      return
    # Smooth scroll for the navigation menu and links with .scrollto classes
    scrolltoOffset = $('#header').outerHeight() - 1
    $(document).on 'click', '.nav-menu a, .mobile-nav a, .scrollto', (e) ->
      if location.pathname.replace(/^\//, '') == @pathname.replace(/^\//, '') and location.hostname == @hostname
        target = $(@hash)
        if target.length
          e.preventDefault()
          scrollto = target.offset().top - scrolltoOffset
          if $(this).attr('href') == '#header'
            scrollto = 0
          $('html, body').animate { scrollTop: scrollto }, 1500, 'easeInOutExpo'
          if $(this).parents('.nav-menu, .mobile-nav').length
            $('.nav-menu .active, .mobile-nav .active').removeClass 'active'
            $(this).closest('li').addClass 'active'
          if $('body').hasClass('mobile-nav-active')
            $('body').removeClass 'mobile-nav-active'
            $('.mobile-nav-toggle i').toggleClass 'icofont-navigation-menu icofont-close'
            $('.mobile-nav-overly').fadeOut()
          return false
      return
    # Activate smooth scroll on page load with hash links in the url
    $(document).ready ->
      if window.location.hash
        initial_nav = window.location.hash
        if $(initial_nav).length
          scrollto = $(initial_nav).offset().top - scrolltoOffset
          $('html, body').animate { scrollTop: scrollto }, 1500, 'easeInOutExpo'
      return
    # Navigation active state on scroll
    nav_sections = $('section')
    main_nav = $('.nav-menu, .mobile-nav')
    $(window).on 'scroll', ->
      cur_pos = $(this).scrollTop() + 200
      nav_sections.each ->
        top = $(this).offset().top
        bottom = top + $(this).outerHeight()
        if cur_pos >= top and cur_pos <= bottom
          if cur_pos <= bottom
            main_nav.find('li').removeClass 'active'
          main_nav.find('a[href="#' + $(this).attr('id') + '"]').parent('li').addClass 'active'
        if cur_pos < 300
          $('.nav-menu ul:first li:first, .mobile-nav ul:first li:first').addClass 'active'
        return
      return
    # Mobile Navigation
    if $('.nav-menu').length
      $mobile_nav = $('.nav-menu').clone().prop(class: 'mobile-nav d-lg-none')
      $('body').append $mobile_nav
      $('body').prepend '<button type="button" class="mobile-nav-toggle d-lg-none"><i class="icofont-navigation-menu"></i></button>'
      $('body').append '<div class="mobile-nav-overly"></div>'
      $(document).on 'click', '.mobile-nav-toggle', (e) ->
        $('body').toggleClass 'mobile-nav-active'
        $('.mobile-nav-toggle i').toggleClass 'icofont-navigation-menu icofont-close'
        $('.mobile-nav-overly').toggle()
        return
      $(document).on 'click', '.mobile-nav .drop-down > a', (e) ->
        e.preventDefault()
        $(this).next().slideToggle 300
        $(this).parent().toggleClass 'active'
        return
      $(document).click (e) ->
        container = $('.mobile-nav, .mobile-nav-toggle')
        if !container.is(e.target) and container.has(e.target).length == 0
          if $('body').hasClass('mobile-nav-active')
            $('body').removeClass 'mobile-nav-active'
            $('.mobile-nav-toggle i').toggleClass 'icofont-navigation-menu icofont-close'
            $('.mobile-nav-overly').fadeOut()
        return
    else if $('.mobile-nav, .mobile-nav-toggle').length
      $('.mobile-nav, .mobile-nav-toggle').hide()
    # Toggle .header-scrolled class to #header when page is scrolled
    $(window).scroll ->
      if $(this).scrollTop() > 100
        $('#header').addClass 'header-scrolled'
        $('#topbar').addClass 'topbar-scrolled'
      else
        $('#header').removeClass 'header-scrolled'
        $('#topbar').removeClass 'topbar-scrolled'
      return
    if $(window).scrollTop() > 100
      $('#header').addClass 'header-scrolled'
      $('#topbar').addClass 'topbar-scrolled'
    # Back to top button
    $(window).scroll ->
      if $(this).scrollTop() > 100
        $('.back-to-top').fadeIn 'slow'
      else
        $('.back-to-top').fadeOut 'slow'
      return
    $('.back-to-top').click ->
      $('html, body').animate { scrollTop: 0 }, 1500, 'easeInOutExpo'
      false
    # jQuery counterUp
    $('[data-toggle="counter-up"]').counterUp
      delay: 10
      time: 1000
    # Testimonials carousel (uses the Owl Carousel library)
    $('.testimonials-carousel').owlCarousel
      autoplay: true
      dots: true
      loop: true
      responsive:
        0: items: 1
        768: items: 1
        900: items: 2
    # Initiate the venobox plugin
    $(document).ready ->
      $('.venobox').venobox()
      return
    # Initiate the datepicker plugin
    $(document).ready ->
      $('.datepicker').datepicker autoclose: true
      return
    return
  )(jQuery)

  ko.applyBindings {vm}
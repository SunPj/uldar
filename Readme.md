# Uldar CMS

Uldar is a tiny library that can be used to extend any custom application to bring CMS functionality

## About

Uldar CMS is a tiny Scala library which values providing full JSON API on backend side, that can be easily extended
using a number of plugins from [extension collection](https://uldar.com/extensions) or implementing custom ones.

It does not depend on specific database, backend or fronend framework, so you free to use the one that fits you best 

Most of extensions come with samples covering all popular databases (MySQL, PostgreSQL) and frontend libraries such as AngularJs, ReactJs, VueJs, etc
so you have a choice and can find the one you need.  

## Motivation
Providing user with a high level API to build the app using UI elements is a nice idea, but in the same time it does not scale 
enough when we come to business specific needs where general web components don't fit well. 

Low level API can be used to build robust and neat logic but there is always a place to provide user with tools to change business specific information 
without touching the code.

We think good CMS engine should help developer to raise the API to UI tools, end users can use, without restricting the code base
to follow CMS structure and eliminate the case when user totally end up with being embraced with CMS restrictions.

## Abstraction 

Uldar introduces two main abstractions `Widget` and `ApiExtension`, the application can be extended with either

#### Widget

A `Widget` is the smallest fundamental building block of UI. Although there is no direct way to manipulate widgets via UI (there is only program API),
they are still considered to be the option of extending an application.

`Widget` is recursive structure meaning it can consists of list of other `widgets` and so on.

Some samples of widgets: 
1. Block of news (configuration might look like: filtration by tags, subject, topic, views count, etc)
2. Subscribe form (configuration might look like: plainId, text, coupon, etc)
3. Show only block that has nested widgets but shown only for Subscribers/User roles (configuration: roleId, accessLevel, etc)
4. Image carousel (configuration: filtration by tag, limit, ect)
5. Popular goods (configuration: filtration by category, price, ect)
 
`Widget` is a notion of composition of logic on both frontend and backend

`Widget` is an abstract term assumed combination of `WidgetDataProvider`, `WidgetRenderer` and `WidgetConfigurator`

`WidgetConfigurator` is UI component implemented on frontend, that used by user to define and configure widget 

`WidgetDataProvider` is backend service which provides `data model` to render the widget (using configuration), and process all widget API calls 

`WidgetRenderer` is UI component which is in charge of rendering the widget using `render data model` on page

#### ApiExtension

Using `ApiExtension` is a way to expand backend API with new functionality, there is no any specific restriction about its implementation

## CMS extensions 

Extension is a reusable collection of functionality that can be shared and reused by others.  

Extension can bring set of `ApiExtension` and `WidgetDataProvider` implementations.

You can find most popular extensions on [official page](https://uldar.com/extensions)

Feel free to implement your own extension following the [developers guide](https://uldar.com/write-you-own-extensions)

## Integration

You can't use an Uldar CMS as it is, because it is just a library.

Though integration is quite straightforward, you need to bind `ExtensionService.processApiCall` to HTTP call (say using `/uldar/*` url)  

There are some existing integration libraries you can use for [PlayFramework](https://uldar.com/playframework), [SpringMVC](https://uldar.com/springmvc), [http4s](https://uldar.com/http4s)
 
# Uldar CMS

Uldar is a tiny library that can be used to extend any custom application to bring CMS functionality

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

## Extending the application using plugins && reuse code 

Extension is a reusable collection of functionality that can be shared and reused by others.  

Extension can bring `ApiExtension` implementations and custom `widgets` have multiple implementation for different frontend libraries
(such as AngularJs/ReactJs/VueJs/etc) and databases

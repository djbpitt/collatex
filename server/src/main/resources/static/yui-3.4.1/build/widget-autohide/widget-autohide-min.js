/*
YUI 3.4.1 (build 4118)
Copyright 2011 Yahoo! Inc. All rights reserved.
Licensed under the BSD License.
http://yuilibrary.com/license/
*/
YUI.add("widget-autohide",function(a){var o="widgetAutohide",m="autohide",k="clickoutside",c="focusoutside",l="document",e="key",p="esc",f="bindUI",n="syncUI",i="rendered",j="boundingBox",h="visible",d="Change",b=a.ClassNameManager.getClassName;function g(q){a.after(this._bindUIAutohide,this,f);a.after(this._syncUIAutohide,this,n);if(this.get(i)){this._bindUIAutohide();this._syncUIAutohide();}}g.ATTRS={hideOn:{valueFn:function(){return[{node:a.one(l),eventName:e,keyCode:p}];},validator:a.Lang.isArray}};g.prototype={_uiHandlesAutohide:null,destructor:function(){this._detachUIHandlesAutohide();},_bindUIAutohide:function(){this.after(h+d,this._afterHostVisibleChangeAutohide);this.after("hideOnChange",this._afterHideOnChange);},_syncUIAutohide:function(){this._uiSetHostVisibleAutohide(this.get(h));},_uiSetHostVisibleAutohide:function(q){if(q){a.later(1,this,"_attachUIHandlesAutohide");}else{this._detachUIHandlesAutohide();}},_attachUIHandlesAutohide:function(){if(this._uiHandlesAutohide){return;}var w=this.get(j),u=a.bind(this.hide,this),r=[],q=this,s=this.get("hideOn"),t=0,v={node:undefined,ev:undefined,keyCode:undefined};for(;t<s.length;t++){v.node=s[t].node;v.ev=s[t].eventName;v.keyCode=s[t].keyCode;if(!v.node&&!v.keyCode&&v.ev){r.push(w.on(v.ev,u));}else{if(v.node&&!v.keyCode&&v.ev){r.push(v.node.on(v.ev,u));}else{if(v.node&&v.keyCode&&v.ev){r.push(v.node.on(v.ev,u,v.keyCode));}else{}}}}this._uiHandlesAutohide=r;},_detachUIHandlesAutohide:function(){a.each(this._uiHandlesAutohide,function(q){q.detach();});this._uiHandlesAutohide=null;},_afterHostVisibleChangeAutohide:function(q){this._uiSetHostVisibleAutohide(q.newVal);},_afterHideOnChange:function(q){this._detachUIHandlesAutohide();if(this.get(h)){this._attachUIHandlesAutohide();}}};a.WidgetAutohide=g;},"3.4.1",{requires:["base-build","widget","event-outside","event-key"]});
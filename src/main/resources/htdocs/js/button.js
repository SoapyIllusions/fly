(function(Button) {
  
  Button.Model = Backbone.Model.extend({
      click: function() {
        var that = this
          , data = {}
          ;
        
        _.each(this.get('params'), function(param) {
          data[param] = fly.mem.get(param);
        });
        
        fly.api('GET', this.get('url'), data, function(results) {
          fly.mem.set(that.get('bind'), results);
        });
      }
  
    , initialize: function(options) {
        var $node = $(options.node)
          , paramStr = $node.data('param')
          , params = []
          ;
        
        _.each(paramStr.split(','), function(param) {
          params.push($.trim(param));
        });
        
        this.set('url', $node.data('url'));
        this.set('params', params);
        this.set('bind', $node.data('bind'));
      }
  });
  
  Button.View = Backbone.View.extend({
      events: {
        'click': 'click'
      }
  
    , click: function() {
        this.model.click();
      }
    
    , render: function() {
        return this;
      }
      
    , initialize: function() {
        _.bindAll(this);
        this.$el = this.model.get('node');
      }
  });
  
})(fly.module('button'));
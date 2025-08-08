const link = document.createElement("link");

const title = document.createElement("title");

document.body.style.backgroundImage = "url(https://i.imgur.com/fS6QDfn.png)";

document.body.style.backgroundRepeat = "no-repeat";

document.body.style.backgroundSize = "900px 500px";

document.body.style.backgroundPosition = "300px 100px";

link.rel = "icon";

link.href = "https://cdn.pixabay.com/photo/2017/06/10/07/18/list-2389219_1280.png";

link.type = "image/png";

title.textContent = "Todo App";

document.head.appendChild(link);

document.head.appendChild(title);

const cancel_button = document.getElementById('cancel');

const confirm_button = document.getElementById('confirm');

const new_url = 'http://localhost:8080/unsubscribe?confirmed=:Confirmed';

var stop = false;

cancel_button.addEventListener('click', (e) => {
    
 e.preventDefault();
    
 history.back()});

 confirm_button.addEventListener('click', (e) => {
     
  const original_html = e.target.parentElement;
                                             
  const clock_element = document.createElement('p');
                                    
  clock_element.textContent = 30;
                                    
  const cancel = document.createElement('button');
                                    
  let timeout;
     
  clock_element.style.position = "absolute";
     
  clock_element.style.transform = "translate(87px, 10px)";
     
  cancel.style.position = "absolute";
     
  cancel.style.transform  = "translate(64px, 60px)";  
                                    
  clock_element.addEventListener('activate', (e) => { 
                                                                              
  let this_current = parseInt(e.target.textContent);
                                                                              
  stop = false;
                                                                              
  const interval = setInterval((function () {
      
                    if (this_current != -1 && stop != true) {
                        
                     e.target.textContent = this_current--; 
                    }
      
                    else { if (stop) {
                        
                            clearInterval(interval)
                    }
                    else { clearInterval(interval)}}}), 1000);
                                                                              
  timeout = setTimeout((function () {
      
             if (!stop) {
                 
              window.location.href = new_url}}), 32000)
                                                                          
  });
     
  cancel.style.width = '60px';
     
  cancel.style.height = '30px';
     
  cancel.textContent = 'Cancel';
     
  cancel.addEventListener('click', (e) => {
      
   e.preventDefault();
      
   stop = true;
      
   clearTimeout(timeout);
      
   e.target.parentElement.replaceWith(original_html)})
     
   const new_div = document.createElement('div');
     
   new_div.style.position = 'absolute';
     
   new_div.style.transform = 'translate(650px, 200px)';
     
   new_div.appendChild(clock_element);
     
   new_div.appendChild(cancel);
     
   e.target.parentElement.replaceWith(new_div);
     
   const event = new Event('activate');
     
   clock_element.dispatchEvent(event);
 
 }, {passive: true});
package eu.homecredit.openapi.onboarding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.MediaType;


@RestController
public class ArticleController {

    private final AtomicLong articleCounter = new AtomicLong();
    private final AtomicLong commentCounter = new AtomicLong();
    private static List<Article> articles = new ArrayList<Article>();

    //-------------------Retrieve Single Article--------------------------------------------------------

    @RequestMapping(value = "/articles/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getArticle(@PathVariable("id") long id) {

        Article article = findArticleById(id);
        if (article == null) {
            return new ResponseEntity<Article>(HttpStatus.NOT_FOUND);
        }

        ReturnArticleDetail a = new ReturnArticleDetail(article.getId(), article.getAuthor(), article.getTitle(), article.getCreation_date(), article.getUpdated_date(), article.getUrl(), article.getContent(), article.getCommentCount());
        return new ResponseEntity<ReturnArticleDetail>(a, HttpStatus.OK);
    }

    //-------------------Retrieve All Articles--------------------------------------------------------

    @RequestMapping(value = "/articles", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> listAllArticles() {
        if(articles.isEmpty()){
            return new ResponseEntity<List<Article>>(HttpStatus.NO_CONTENT);
        }

        List<ReturnArticleList> returnList = new ArrayList<ReturnArticleList>();
        for (Article article : articles) {
            ReturnArticleList a = new ReturnArticleList(article.getId(), article.getAuthor(), article.getTitle(), article.getCreation_date(), article.getUpdated_date(), article.getUrl(), article.getCommentCount());
            returnList.add(a);
        }

        return new ResponseEntity<List<ReturnArticleList>>(returnList, HttpStatus.OK);
    }

    //-------------------Create an Article--------------------------------------------------------

    @RequestMapping(value = "/articles", method = RequestMethod.POST)
    public ResponseEntity<Article> createArticle(@RequestBody Article article, UriComponentsBuilder ucBuilder) {

        // update & store article
        article.setId(articleCounter.incrementAndGet());
        article.setCreation_date(getCurrentDateAndTime());
        article.setUpdated_date(null);
        article.setUrl("/articles/"+article.getId());
        articles.add(article);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path("/articles/{id}").buildAndExpand(article.getId()).toUri());
        return new ResponseEntity<Article>(article, headers, HttpStatus.CREATED);
    }

    //-------------------Update Article--------------------------------------------------------

    @RequestMapping(value = "/articles/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Article> updateArticle(@RequestBody Article updatedArticle, @PathVariable("id") long id) {

        Article article = findArticleById(id);
        if (article == null) {
            return new ResponseEntity<Article>(HttpStatus.NOT_FOUND);
        }

        // update article
        article.setTitle(updatedArticle.getTitle());
        article.setContent(updatedArticle.getContent());
        article.setUpdated_date(getCurrentDateAndTime());

        return new ResponseEntity<Article>(article, HttpStatus.OK);
    }

    //-------------------Retrieve All Article Comments--------------------------------------------------------

    @RequestMapping(value = "/articles/{id}/comments", method = RequestMethod.GET, produces=MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody ResponseEntity<?> listAllArticleComments(@PathVariable("id") long id) {
        Article article = findArticleById(id);
        if (article == null) {
            return new ResponseEntity<Article>(HttpStatus.NOT_FOUND);
        }

        if(article.getComments().isEmpty()){
            return new ResponseEntity<List<Comment>>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<List<Comment>>(article.getComments(), HttpStatus.OK);
    }

    //-------------------Create a Comment--------------------------------------------------------

    @RequestMapping(value = "/articles/{id}/comments", method = RequestMethod.POST)
    public ResponseEntity<?> createComment(@RequestBody Comment comment, @PathVariable("id") long id, UriComponentsBuilder ucBuilder) {

        Article article = findArticleById(id);
        if (article == null) {
            return new ResponseEntity<Article>(HttpStatus.NOT_FOUND);
        }

        // update & store comment
        comment.setId(commentCounter.incrementAndGet());
        comment.setCreation_date(getCurrentDateAndTime());
        comment.setUrl("/articles/"+id+"/comments/"+comment.getId());
        article.getComments().add(comment);
        article.setCommentCount(article.getCommentCount()+1);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(ucBuilder.path(comment.getUrl()).buildAndExpand().toUri());
        return new ResponseEntity<Comment>(comment, headers, HttpStatus.CREATED);
    }

    //-------------------Delete a Comment--------------------------------------------------------

    @RequestMapping(value = "/articles/{articleId}/comments/{commentId}", method = RequestMethod.DELETE)
    public @ResponseBody ResponseEntity<?> deleteComment(@PathVariable("articleId") long articleId, @PathVariable("commentId") long commentId) {
        Article article = findArticleById(articleId);
        if (article == null) {
            return new ResponseEntity<Article>(HttpStatus.NOT_FOUND);
        }

        Comment comment = findCommentById(article.getComments(),commentId);
        if (comment == null) {
            return new ResponseEntity<Article>(HttpStatus.NOT_FOUND);
        }

        // remove comment from list and decrease counter
        deleteCommentById(article.getComments(),commentId);
        article.setCommentCount(article.getCommentCount()-1);

        return new ResponseEntity<List<Comment>>(article.getComments(), HttpStatus.NO_CONTENT);
    }

    
    Article findArticleById(long id) {
        for(Article article : articles){
            if(article.getId() == id){
                return article;
            }
        }
        return null;
    }

    Comment findCommentById(List<Comment>comments, long id) {
        for(Comment comment : comments){
            if(comment.getId() == id){
                return comment;
            }
        }
        return null;
    }

    public void deleteCommentById(List<Comment>comments, long id) {

        for (Iterator<Comment> iterator = comments.iterator(); iterator.hasNext(); ) {
            Comment comment = iterator.next();
            if (comment.getId() == id) {
                iterator.remove();
            }
        }
    }

    String getCurrentDateAndTime () {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return sdf.format(Calendar.getInstance().getTime());
    }
}


// aux classes for returning JSONs

class ReturnArticleList {   // does not contain Content and Comments
    public long id;
    public String author;
    public String title;
    public String creation_date;
    public String updated_date;
    public String url;
    public int commentCount;

    public ReturnArticleList(long id, String author, String title, String creation_date, String updated_date, String url, int commentCount) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.creation_date = creation_date;
        this.updated_date = updated_date;
        this.url = url;
        this.commentCount = commentCount;
    }
}

class ReturnArticleDetail {     // does not contain Comments
    public long id;
    public String author;
    public String title;
    public String creation_date;
    public String updated_date;
    public String url;
    public String content;
    public int commentCount;

    public ReturnArticleDetail(long id, String author, String title, String creation_date, String updated_date, String url, String content, int commentCount) {
        this.id = id;
        this.author = author;
        this.title = title;
        this.creation_date = creation_date;
        this.updated_date = updated_date;
        this.url = url;
        this.content = content;
        this.commentCount = commentCount;
    }
}

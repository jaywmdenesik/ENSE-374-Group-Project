package fi.hut.soberit.agilefant.business;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import fi.hut.soberit.agilefant.business.impl.RankingBusinessImpl;
import fi.hut.soberit.agilefant.business.impl.StoryBusinessImpl;
import fi.hut.soberit.agilefant.db.HourEntryDAO;
import fi.hut.soberit.agilefant.db.IterationDAO;
import fi.hut.soberit.agilefant.db.StoryDAO;
import fi.hut.soberit.agilefant.db.UserDAO;
import fi.hut.soberit.agilefant.exception.ObjectNotFoundException;
import fi.hut.soberit.agilefant.exception.OperationNotPermittedException;
import fi.hut.soberit.agilefant.model.Backlog;
import fi.hut.soberit.agilefant.model.Iteration;
import fi.hut.soberit.agilefant.model.Product;
import fi.hut.soberit.agilefant.model.Project;
import fi.hut.soberit.agilefant.model.Story;
import fi.hut.soberit.agilefant.model.StoryHourEntry;
import fi.hut.soberit.agilefant.model.StoryState;
import fi.hut.soberit.agilefant.model.Task;
import fi.hut.soberit.agilefant.model.User;
import fi.hut.soberit.agilefant.transfer.StoryTO;
import fi.hut.soberit.agilefant.util.HourEntryHandlingChoice;
import fi.hut.soberit.agilefant.util.TaskHandlingChoice;

public class StoryBusinessTest {

    StoryBusinessImpl storyBusiness;
    
    StoryDAO storyDAO;
    IterationDAO iterationDAO;
    UserDAO userDAO;
    
    BacklogBusiness backlogBusiness;
    ProjectBusiness projectBusiness;
    BacklogHistoryEntryBusiness blheBusiness;
    IterationHistoryEntryBusiness iheBusiness;
    RankingBusiness rankingBusiness;
    TransferObjectBusiness transferObjectBusiness;
    HourEntryDAO hourEntryDAO;
    TaskBusiness taskBusiness;
    HourEntryBusiness hourEntryBusiness;
    
    
    Backlog backlog;
    Iteration iteration;
    Story story1;
    Story story2;
    
    User assignedUser; 
    
    Story storyInIteration;
    Story storyInProject;
    Story storyInProduct;
    
    Boolean storyPriorityUpdated;
    Boolean storyBacklogUpdated;
    
    @Before
    public void setUp() {
        backlog = new Product();
        iteration = new Iteration();
        iteration.setId(5834);
        story1 = new Story();
        story1.setId(666);
        story2 = new Story();
        
        storyPriorityUpdated = false;
        storyBacklogUpdated = false;
    }
    
    @Before
    public void setUp_dependencies() {
        storyBusiness = new StoryBusinessImpl();
        
        backlogBusiness = createMock(BacklogBusiness.class);
        storyBusiness.setBacklogBusiness(backlogBusiness);
        
        storyDAO = createMock(StoryDAO.class);
        storyBusiness.setStoryDAO(storyDAO);
        
        iterationDAO = createMock(IterationDAO.class);
        storyBusiness.setIterationDAO(iterationDAO);

        userDAO = createMock(UserDAO.class);
        storyBusiness.setUserDAO(userDAO);
        
        projectBusiness = createMock(ProjectBusiness.class);
        storyBusiness.setProjectBusiness(projectBusiness);
        
        blheBusiness = createMock(BacklogHistoryEntryBusiness.class);
        storyBusiness.setBacklogHistoryEntryBusiness(blheBusiness);
        
        iheBusiness = createMock(IterationHistoryEntryBusiness.class);
        storyBusiness.setIterationHistoryEntryBusiness(iheBusiness);
        
        rankingBusiness = new RankingBusinessImpl();
        storyBusiness.setRankingBusiness(rankingBusiness);
        
        transferObjectBusiness = createMock(TransferObjectBusiness.class);
        storyBusiness.setTransferObjectBusiness(transferObjectBusiness);
        
        hourEntryDAO = createMock(HourEntryDAO.class);
        storyBusiness.setHourEntryDAO(hourEntryDAO);
        
        taskBusiness = createMock(TaskBusiness.class);
        storyBusiness.setTaskBusiness(taskBusiness);
        
        hourEntryBusiness = createMock(HourEntryBusiness.class);
        storyBusiness.setHourEntryBusiness(hourEntryBusiness);
        
    }
    
    @Before
    public void setUpStorysProjectResponsiblesData() {
        Iteration iter = new Iteration();
        Project proj = new Project();
        Product prod = new Product();
        iter.setParent(proj);
        proj.setParent(prod);
        
        assignedUser = new User();
        assignedUser.setId(2233);
        
        storyInIteration = new Story();
        storyInProject = new Story();
        storyInProduct = new Story();
        
        storyInIteration.setId(868);
        storyInProduct.setId(951);
        storyInProject.setId(3);
        
        storyInIteration.setBacklog(iter);
        storyInProject.setBacklog(proj);
        storyInProduct.setBacklog(prod);
    }

    private void replayAll() {
        replay(backlogBusiness, storyDAO, iterationDAO, userDAO, projectBusiness, iheBusiness, blheBusiness, transferObjectBusiness, hourEntryDAO, taskBusiness, hourEntryBusiness);
    }
    
    private void verifyAll() {
        verify(backlogBusiness, storyDAO, iterationDAO, userDAO, projectBusiness, iheBusiness, blheBusiness, transferObjectBusiness, hourEntryDAO, taskBusiness, hourEntryBusiness);
    }

    
    @Test
    public void testGetStoriesByBacklog() {
        List<Story> storiesList = Arrays.asList(story1, story2);
        expect(storyDAO.getStoriesByBacklog(backlog)).andReturn(storiesList);
        replayAll();
        
        assertSame(storiesList, storyBusiness.getStoriesByBacklog(backlog));
        
        verifyAll();
    }
       
    @Test
    public void testGetStoryContents_delegate() {
        expect(storyDAO.get(story1.getId())).andReturn(story1);
        expect(iterationDAO.get(iteration.getId()));
    }
    
    @Test
    public void testGetStoryContents() {
        Task task1 = new Task();
        Task task2 = new Task();
        task2.setStory(story1);
        story1.setBacklog(iteration);
        expect(iterationDAO.getAllTasksForIteration(iteration))
            .andReturn(Arrays.asList(task1, task2));
        replayAll();
        assertTrue(storyBusiness.getStoryContents(story1, iteration)
                .contains(task2));
        verifyAll();
    }
    

    @Test
    public void testGetStorysProjectResponsibles_iteration() {
        Collection<User> assignedUsers = Arrays.asList(assignedUser);
        expect(projectBusiness.getAssignedUsers((Project)storyInIteration.getBacklog().getParent()))
            .andReturn(assignedUsers);
        replayAll();
        
        assertEquals(assignedUsers, storyBusiness.getStorysProjectResponsibles(storyInIteration));
        
        verifyAll();
    }
    
    @Test
    public void testGetStorysProjectResponsibles_project() {
        Collection<User> assignedUsers = Arrays.asList(assignedUser);
        expect(projectBusiness.getAssignedUsers((Project)storyInProject.getBacklog()))
            .andReturn(Arrays.asList(assignedUser));
        replayAll();
        
        assertEquals(assignedUsers, storyBusiness.getStorysProjectResponsibles(storyInProject));
        
        verifyAll();
    }
    
    @Test
    public void testGetStorysProjectResponsibles_product() {
        replayAll();       
        assertEquals(0, storyBusiness.getStorysProjectResponsibles(storyInProduct).size());
        verifyAll();
    }

    
    @Test
    public void testGetStoryPointSumByBacklog() {
        expect(storyDAO.getStoryPointSumByBacklog(backlog.getId()))
            .andReturn(6);
        replayAll();
        
        assertEquals(6, storyBusiness.getStoryPointSumByBacklog(backlog));
        
        verifyAll();
    }
    
    @Test
    public void testMoveStoryToBacklog() {
        Backlog oldBacklog = new Iteration();
        oldBacklog.setId(8482);
        Backlog newBacklog = new Iteration();
        newBacklog.setId(1904);
        Story movable = new Story();
        
        oldBacklog.setStories(new HashSet<Story>(Arrays.asList(movable)));
        movable.setBacklog(oldBacklog);
        
        Story last = new Story();
        last.setRank(123);
        
        storyDAO.store(isA(Story.class));
        expect(backlogBusiness.retrieve(1904)).andReturn(newBacklog);
        expect(storyDAO.getLastStoryInRank(newBacklog)).andReturn(last);
        
        blheBusiness.updateHistory(oldBacklog.getId());
        blheBusiness.updateHistory(newBacklog.getId());
        
        iheBusiness.updateIterationHistory(oldBacklog.getId());
        iheBusiness.updateIterationHistory(newBacklog.getId());
                
        replayAll();
        storyBusiness.moveStoryToBacklog(movable, newBacklog);
        verifyAll();
        
        assertTrue(newBacklog.getStories().contains(movable));
        assertFalse(oldBacklog.getStories().contains(movable));
        assertEquals(newBacklog, movable.getBacklog());
        assertEquals(124, movable.getRank());
    }
    
    @Test(expected = OperationNotPermittedException.class)
    public void testMoveStoryToBacklog_withChildrenToAnotherProduct() {
        Product oldParent = new Product();
        Backlog oldBacklog = new Project();
        oldBacklog.setId(8482);
        oldBacklog.setParent(oldParent);
        
        Product newBacklog = new Product();
        newBacklog.setId(1904);
        
        Story movable = new Story();
        movable.setBacklog(oldBacklog);
        
        movable.setChildren(new HashSet<Story>(Arrays.asList(new Story(), new Story())));

        expect(backlogBusiness.getParentProduct(oldBacklog)).andReturn(oldParent);
        expect(backlogBusiness.getParentProduct(newBacklog)).andReturn(newBacklog);
        replayAll();
        storyBusiness.moveStoryToBacklog(movable, newBacklog);
        verifyAll();
    }
    
    @Test
    public void testMoveStoryToBacklog_withChildrenToSameProduct() {
        Product newBacklog = new Product();
        newBacklog.setId(1904);
        
        Backlog oldBacklog = new Iteration();
        oldBacklog.setId(8482);
        Project project = new Project();
        project.setParent(new Product());
        oldBacklog.setParent(newBacklog);
        
        
        Story movable = new Story();
        movable.setBacklog(oldBacklog);
        
        movable.setChildren(new HashSet<Story>(Arrays.asList(new Story(), new Story())));

        expect(backlogBusiness.getParentProduct(oldBacklog)).andReturn(newBacklog);
        expect(backlogBusiness.getParentProduct(newBacklog)).andReturn(newBacklog);
        
        storyDAO.store(isA(Story.class));
        expect(backlogBusiness.retrieve(1904)).andReturn(newBacklog);
        expect(storyDAO.getLastStoryInRank(newBacklog)).andReturn(new Story());
        
        blheBusiness.updateHistory(oldBacklog.getId());
        blheBusiness.updateHistory(newBacklog.getId());
        
        iheBusiness.updateIterationHistory(oldBacklog.getId());
        
        replayAll();
        storyBusiness.moveStoryToBacklog(movable, newBacklog);
        verifyAll();
    }
    
    @Test
    public void testMoveStoryToBacklog_parentStoryUnderDifferentProduct() {
        Product newBacklog = new Product();
        newBacklog.setId(1904);
        
        Backlog oldBacklog = new Iteration();
        oldBacklog.setId(8482);
        Project project = new Project();
        project.setParent(new Product());
        
        Story parentStory = new Story();
        parentStory.setBacklog(oldBacklog);
        
        Story movable = new Story();
        movable.setBacklog(oldBacklog);
        
        movable.setParent(parentStory);

        expect(backlogBusiness.getParentProduct(oldBacklog)).andReturn(new Product());
        expect(backlogBusiness.getParentProduct(newBacklog)).andReturn(newBacklog);
        
        storyDAO.store(isA(Story.class));
        expect(backlogBusiness.retrieve(1904)).andReturn(newBacklog);
        expect(storyDAO.getLastStoryInRank(newBacklog)).andReturn(new Story());
        
        blheBusiness.updateHistory(oldBacklog.getId());
        blheBusiness.updateHistory(newBacklog.getId());
        
        iheBusiness.updateIterationHistory(oldBacklog.getId());
        
        replayAll();
        storyBusiness.moveStoryToBacklog(movable, newBacklog);
        verifyAll();
        
        assertNull(movable.getParent());
    }
    
    
    private void store_createMockStoryBusiness() {       
        this.storyBusiness = new StoryBusinessImpl() {
            @Override
            public void moveStoryToBacklog(Story story, Backlog backlog) {
                storyBacklogUpdated = true;
            }
            
//            @Override
//            public void updateStoryPriority(Story story, int insertAtPriority) {
//                storyPriorityUpdated = true;
//            }
        };
        backlogBusiness = createMock(BacklogBusiness.class);
        storyBusiness.setBacklogBusiness(backlogBusiness);
        
        storyDAO = createMock(StoryDAO.class);
        storyBusiness.setStoryDAO(storyDAO);
        
        userDAO = createMock(UserDAO.class);
        storyBusiness.setUserDAO(userDAO);
        
        blheBusiness = createMock(BacklogHistoryEntryBusiness.class);
        storyBusiness.setBacklogHistoryEntryBusiness(blheBusiness);
    }
    
    @Test
    public void testStore_updateResponsibles() {
        this.store_createMockStoryBusiness();
        
        Backlog backlog = storyInIteration.getBacklog();
        User user1 = new User();
        User user2 = new User();
        Set<User> users = new HashSet<User>(Arrays.asList(user1, user2));
        
        expect(storyDAO.get(storyInIteration.getId())).andReturn(storyInIteration);
        expect(userDAO.get(123)).andReturn(user1);
        expect(userDAO.get(222)).andReturn(user2);
        storyDAO.store(EasyMock.isA(Story.class));
        
        Story dataItem = new Story();
        dataItem.setName("Foo item");
        dataItem.setDescription("Fubar");
        dataItem.setStoryPoints(333);
        dataItem.setState(StoryState.PENDING);
        dataItem.setRank(222);
        
        blheBusiness.updateHistory(storyInIteration.getBacklog().getId());
        
        replayAll();
        Story actual = storyBusiness.store(storyInIteration.getId(),
                dataItem, null, new HashSet<Integer>(Arrays.asList(123, 222)));
        verifyAll();
        
        assertSame("The backlogs don't match", backlog, actual.getBacklog());
        assertEquals("The responsibles don't match", users, actual.getResponsibles());
        
        assertEquals(dataItem.getName(), actual.getName());
        assertEquals(dataItem.getDescription(), actual.getDescription());
        assertEquals(dataItem.getStoryPoints(), actual.getStoryPoints());
        assertEquals(dataItem.getState(), actual.getState());
        
        assertFalse(storyBacklogUpdated);
    }
    
    
    @Test(expected = IllegalArgumentException.class)
    public void testStore_nullStoryId() {
        this.store_createMockStoryBusiness();
        storyBusiness.store(null, new Story(), 123, new HashSet<Integer>());
    }
    
    
    @Test(expected = ObjectNotFoundException.class)
    public void testStore_noSuchStory() {
        this.store_createMockStoryBusiness();
        expect(storyDAO.get(222)).andReturn(null);
        replayAll();
        storyBusiness.store(222, new Story(), 123, new HashSet<Integer>());
        verifyAll();
    }
    
    
    @Test
    public void testStore_updateBacklogAndClearResponsibles() {
        this.store_createMockStoryBusiness();
        Backlog newBacklog = new Project();
        newBacklog.setId(123);
        Set<User> users = new HashSet<User>(Arrays.asList(new User(), new User()));
        storyInIteration.setResponsibles(users);
        
        expect(storyDAO.get(storyInIteration.getId())).andReturn(storyInIteration);
        expect(backlogBusiness.retrieve(newBacklog.getId())).andReturn(newBacklog);
        
        storyDAO.store(EasyMock.isA(Story.class));
        
        blheBusiness.updateHistory(storyInIteration.getBacklog().getId());
        
        replayAll();
        Story actual = storyBusiness.store(storyInIteration.getId(),
                new Story(), newBacklog.getId(), new HashSet<Integer>());
        verifyAll();
        
        assertEquals(0, actual.getResponsibles().size());
        
        assertTrue(storyBacklogUpdated);
    }
    
    
    private void expectHistoryUpdates(Backlog blog) {
        if (blog instanceof Iteration) {
            blheBusiness.updateHistory(blog.getId());
            iheBusiness.updateIterationHistory(blog.getId());
        }
        else if (blog instanceof Project) {
            blheBusiness.updateHistory(blog.getId());
        }
    }
    
    
    @Test
    public void testCreateStory_noResponsibles() {
        Backlog blog = new Iteration();
        expect(backlogBusiness.retrieve(5)).andReturn(blog).times(2);
        
        Capture<Story> capturedStory = new Capture<Story>();
        
        Story last = new Story();
        last.setRank(12);
        expect(storyDAO.getLastStoryInRank(isA(Backlog.class))).andReturn(last);
        
        expect(storyDAO.create(EasyMock.capture(capturedStory))).andReturn(88);
        
        expectHistoryUpdates(blog);
        
        Story returnedStory = new Story();
        expect(storyDAO.get(88)).andReturn(returnedStory);
        
        Story dataItem = new Story();
        dataItem.setName("Foofaa");
        dataItem.setDescription("Foofaa");
        dataItem.setStoryPoints(22);
        dataItem.setState(StoryState.STARTED);
        
        replayAll();
        Story actual = this.storyBusiness.create(dataItem, 5, null);
        verifyAll();
        
        assertEquals(actual.getClass(), Story.class);
        assertEquals(blog, capturedStory.getValue().getBacklog());
        
        assertEquals(dataItem.getName(), capturedStory.getValue().getName());
        assertEquals(dataItem.getDescription(), capturedStory.getValue().getDescription());
        assertEquals(dataItem.getStoryPoints(), capturedStory.getValue().getStoryPoints());
        assertEquals(dataItem.getState(), capturedStory.getValue().getState());
        assertEquals(13, capturedStory.getValue().getRank());
    }
    
    @Test
    public void testCreateStory_withResponsibles() {
        User user1 = new User();
        User user2 = new User();
        
        Backlog blog = new Project();
        expect(backlogBusiness.retrieve(5)).andReturn(blog).times(2);
        expect(userDAO.get(2)).andReturn(user1);
        expect(userDAO.get(23)).andReturn(user2);
        
        Capture<Story> capturedStory = new Capture<Story>();
        
        Story last = new Story();
        last.setRank(286);
        expect(storyDAO.getLastStoryInRank(isA(Backlog.class))).andReturn(last);
        
        expect(storyDAO.create(EasyMock.capture(capturedStory))).andReturn(88);
        
        expectHistoryUpdates(blog);
        
        Story returnedStory = new Story();
        expect(storyDAO.get(88)).andReturn(returnedStory);
        
        replayAll();
        Story actual = this.storyBusiness.create(new Story(), 5,
                new HashSet<Integer>(Arrays.asList(2,23)));
        verifyAll();
        
        assertSame(actual, returnedStory);
        assertTrue(capturedStory.getValue().getResponsibles().contains(user1));
        assertTrue(capturedStory.getValue().getResponsibles().contains(user2));
        assertEquals(blog, capturedStory.getValue().getBacklog());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateStory_nullDataItem() {
        this.storyBusiness.create(null, 123, new HashSet<Integer>());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateStory_nullBacklogId() {
        this.storyBusiness.create(new Story(), null, new HashSet<Integer>());
    }
    
    @Test(expected = ObjectNotFoundException.class)
    public void testCreateStory_backlogNotFound() {
        expect(backlogBusiness.retrieve(5)).andThrow(new ObjectNotFoundException());
        this.storyBusiness.create(new Story(), 222, new HashSet<Integer>());
    }
    
    @Test(expected = OperationNotPermittedException.class)
    public void testDelete_withTasks() {
        Task task = new Task();
        story1.getTasks().add(task);
        storyBusiness.delete(story1);
    }
    
    @Test(expected = OperationNotPermittedException.class)
    public void testDelete_withHourEntries() {
        StoryHourEntry he = new StoryHourEntry();
        story1.getHourEntries().add(he);
        storyBusiness.delete(story1);
    }

    @Test
    public void testDelete_taskChoice_move() {
        Task task = new Task();
        storyInIteration.getTasks().add(task);
        expect(storyDAO.get(storyInIteration.getId())).andReturn(storyInIteration);
        expect(taskBusiness.move(task, storyInIteration.getBacklog().getId(), null)).andReturn(task);
        storyDAO.remove(storyInIteration);
        blheBusiness.updateHistory(storyInIteration.getBacklog().getId());
        replayAll();
        storyBusiness.delete(storyInIteration.getId(), TaskHandlingChoice.MOVE, null, null);
        verifyAll();
        assertTrue(storyInIteration.getTasks().isEmpty());
    }

    @Test
    public void testDelete_taskChoice_delete() {
        Task task = new Task();
        storyInIteration.getTasks().add(task);
        expect(storyDAO.get(storyInIteration.getId())).andReturn(storyInIteration);
        hourEntryBusiness.moveToBacklog(task.getHourEntries(), storyInIteration.getBacklog());
        taskBusiness.delete(task.getId(), HourEntryHandlingChoice.MOVE);
        storyDAO.remove(storyInIteration);
        blheBusiness.updateHistory(storyInIteration.getBacklog().getId());
        replayAll();
        storyBusiness.delete(storyInIteration.getId(), TaskHandlingChoice.DELETE, null, HourEntryHandlingChoice.MOVE);
        verifyAll();
        assertNull(task.getStory());
        assertTrue(storyInIteration.getTasks().isEmpty());
    }
    
    @Test
    public void testDelete_hourEntryChoice_move() {
        storyInIteration.getHourEntries().add(new StoryHourEntry());
        expect(storyDAO.get(storyInIteration.getId())).andReturn(storyInIteration);
        hourEntryBusiness.moveToBacklog(storyInIteration.getHourEntries(), storyInIteration.getBacklog());
        storyDAO.remove(storyInIteration);
        blheBusiness.updateHistory(storyInIteration.getBacklog().getId());
        replayAll();
        storyBusiness.delete(storyInIteration.getId(), null, HourEntryHandlingChoice.MOVE, null);
        verifyAll();
        assertTrue(storyInIteration.getHourEntries().isEmpty());
    }
    @Test
    public void testDelete_hourEntryChoice_delete() {
        storyInIteration.getHourEntries().add(new StoryHourEntry());
        expect(storyDAO.get(storyInIteration.getId())).andReturn(storyInIteration);
        hourEntryBusiness.deleteAll(storyInIteration.getHourEntries());
        storyDAO.remove(storyInIteration);
        blheBusiness.updateHistory(storyInIteration.getBacklog().getId());
        replayAll();
        storyBusiness.delete(storyInIteration.getId(), null, HourEntryHandlingChoice.DELETE, null);
        verifyAll();
        assertTrue(storyInIteration.getHourEntries().isEmpty());
    }

    /*
     * RANK TO BOTTOM
     */
    @Test
    public void testRankToBottom() {
        Story last = new Story();
        last.setRank(117);
        Backlog product = new Product();
        product.setId(123);
        expect(backlogBusiness.retrieve(123)).andReturn(product);
        expect(storyDAO.getLastStoryInRank(product)).andReturn(last);
        replayAll();
        Story actual = storyBusiness.rankToBottom(story1, 123);
        verifyAll();
        assertEquals(story1.getId(), actual.getId());
        assertEquals(118, actual.getRank());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRankToBottom_noParentGiven() {
        storyBusiness.rankToBottom(story1, null);
    }
    
    @Test(expected = ObjectNotFoundException.class)
    public void testRankToBottom_noParentFound() {
        expect(backlogBusiness.retrieve(1222)).andThrow(new ObjectNotFoundException());
        replayAll();
        storyBusiness.rankToBottom(story1, 1222);
        verifyAll();
    }

    
    /*
     * RANK UNDER STORY
     */
    
    Story firstInRank;
    Story secondInRank;
    Story thirdInRank;
    Story fourthInRank;
    
    private void createRankUnderStoryTestData() {
       Iteration iter = new Iteration();
        
       firstInRank = new Story();
       secondInRank = new Story();
       thirdInRank = new Story();
       fourthInRank = new Story();
       
       firstInRank.setId(222);
       secondInRank.setId(515);
       thirdInRank.setId(7646);
       fourthInRank.setId(57);
       
       firstInRank.setBacklog(iter);
       secondInRank.setBacklog(iter);
       thirdInRank.setBacklog(iter);
       fourthInRank.setBacklog(iter);
       
       firstInRank.setRank(0);
       secondInRank.setRank(2);
       thirdInRank.setRank(3);
       fourthInRank.setRank(13);
    }
    
    private void checkRanks(int first, int second, int third, int fourth) {
        assertEquals("First item's rank doesn't match", first, firstInRank.getRank());
        assertEquals("Second item's rank doesn't match", second, secondInRank.getRank());
        assertEquals("Third item's rank doesn't match", third, thirdInRank.getRank());
        assertEquals("Fourth item's rank doesn't match", fourth, fourthInRank.getRank());
    }
    
    @Test
    public void testRankUnderStory_bottomToTop() {
        createRankUnderStoryTestData();
        
        expect(storyDAO.getStoriesWithRankBetween(fourthInRank.getBacklog(), 0, 12))
            .andReturn(Arrays.asList(firstInRank, secondInRank, thirdInRank));
        
        replayAll();
        Story actual = storyBusiness.rankUnderStory(fourthInRank, null);
        verifyAll();
        
        assertSame(fourthInRank, actual);
        
        checkRanks(1, 3, 4, 0);
    }
    
    @Test
    public void testRankUnderStory_downwards() {
        createRankUnderStoryTestData();
        
        expect(storyDAO.getStoriesWithRankBetween(fourthInRank.getBacklog(), 1, 3))
            .andReturn(Arrays.asList(secondInRank, thirdInRank));
        
        replayAll();
        Story actual = storyBusiness.rankUnderStory(firstInRank, thirdInRank);
        verifyAll();
        
        assertSame(firstInRank, actual);
        
        checkRanks(3, 1, 2, 13);
    }
    
    @Test
    public void testRankUnderStory_upwards() {
        createRankUnderStoryTestData();
        
        expect(storyDAO.getStoriesWithRankBetween(fourthInRank.getBacklog(), 1, 12))
            .andReturn(Arrays.asList(secondInRank, thirdInRank));
        
        replayAll();
        Story actual = storyBusiness.rankUnderStory(fourthInRank, firstInRank);
        verifyAll();
        
        assertSame(fourthInRank, actual);
        
        checkRanks(0, 3, 4, 1);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRankUnderStory_nullStory() {
        storyBusiness.rankUnderStory(null, new Story());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRankUnderStory_differentParent() {
        Story first = new Story();
        first.setBacklog(new Product());
        Story second = new Story();
        second.setBacklog(new Iteration());
        
        storyBusiness.rankUnderStory(first, second);

    }
    
    
    @Test
    public void testRankAndMove_toTop() {
        createRankUnderStoryTestData();
        Story rankedStory = new Story();
        rankedStory.setBacklog(new Project());
        Backlog expectedParent = new Project();
        expectedParent.setId(123);
        expect(backlogBusiness.retrieve(123)).andReturn(expectedParent);
        storyDAO.store(rankedStory);
        expect(storyDAO.getLastStoryInRank(expectedParent)).andReturn(fourthInRank);
        blheBusiness.updateHistory(0);
        blheBusiness.updateHistory(123);
        expect(storyDAO.getStoriesWithRankBetween(expectedParent, 0, fourthInRank.getRank()))
            .andReturn(Arrays.asList(firstInRank, secondInRank, thirdInRank, fourthInRank));
        replayAll();
        Story actual = storyBusiness.rankAndMove(rankedStory, null, expectedParent);
        verifyAll();
        
        assertEquals(expectedParent, actual.getBacklog());
        assertEquals(0, actual.getRank());
    }
    
    @Test
    public void testStoreBatch() {
        Story s1 = new Story();
        s1.setId(1);
        Story s2 = new Story();
        s2.setId(2);
        Collection<Story> batch = Arrays.asList(s1,s2);
        storyDAO.store(s1);
        storyDAO.store(s2);
        replayAll();
        storyBusiness.storeBatch(batch);
        verifyAll();
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testStoreBatch_nonPersited() {
        Story s1 = new Story();
        Story s2 = new Story();
        Collection<Story> batch = Arrays.asList(s1,s2);
        storyDAO.store(s1);
        storyDAO.store(s2);
        replayAll();
        storyBusiness.storeBatch(batch);
        verifyAll();
    }
    
    @Test
    public void testRetrieveStoryTO() {
        StoryTO storyTo = new StoryTO(story1);
        expect(storyDAO.get(story1.getId())).andReturn(story1);
        expect(transferObjectBusiness.constructStoryTO(story1)).andReturn(storyTo);
        expect(hourEntryDAO.calculateSumByStory(story1.getId())).andReturn(100l);
        replayAll();
        assertEquals(storyTo, storyBusiness.retrieveStoryWithMetrics(story1.getId()));
    }
}
